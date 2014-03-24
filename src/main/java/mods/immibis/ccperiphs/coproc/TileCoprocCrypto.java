package mods.immibis.ccperiphs.coproc;

import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.ILuaObject;

public class TileCoprocCrypto extends TileCoprocBase {
	
	public static final int MAX_ASYMMETRIC_KEY_SIZE = 1024; // to avoid causing lag
	
	private static String encodeBase64(byte[] b) {
		return DatatypeConverter.printBase64Binary(b);
	}
	
	private static byte[] decodeBase64(String s) {
		return DatatypeConverter.parseBase64Binary(s);
	}
	
	private static String bytesToHex(byte[] ba) {
		StringBuilder sb = new StringBuilder();
		for(byte b : ba) {
			if((b & 255) < 16)
				sb.append('0');
			sb.append(Integer.toHexString(b & 255));
		}
		return sb.toString();
	}
	
	public static class LuaKey implements ILuaObject {
		
		private final Key key;
		
		public LuaKey(Key key) {
			this.key = key;
		}

		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			case 0:
				// encrypt(string algorithm, string plaintext) -> string ciphertext-base64
				if(args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof String))
					throw new Exception("expected string, string");
				String algorithm = (String)args[0];
				byte[] plaintext = ((String)args[1]).getBytes(Charset.forName("UTF-8"));
				
				Cipher cipher;
				try {
					cipher = Cipher.getInstance(algorithm);
				} catch(NoSuchAlgorithmException e) {
					throw new Exception("unknown algorithm: "+algorithm);
				} catch(NoSuchPaddingException e) {
					throw new Exception("unknown padding: "+algorithm);
				}
				
				try {
					cipher.init(Cipher.ENCRYPT_MODE, key);
				} catch(InvalidKeyException e) {
					throw new Exception("invalid key for algorithm: "+e.getMessage());
				}
				
				byte[] ciphertext;
				
				try {
					ciphertext = cipher.doFinal(plaintext);
				} catch(IllegalBlockSizeException e) {
					throw new Exception("illegal block size: "+e.getMessage());
				}
				
				AlgorithmParameters params = cipher.getParameters();
				if(params != null)
					return new Object[] {params.getAlgorithm() + ":" + encodeBase64(params.getEncoded()) + ":" + encodeBase64(ciphertext)};
				else
					return new Object[] {encodeBase64(ciphertext)};
			
			case 1:
				// decrypt(string algorithm, string ciphertext-base64) -> string plaintext
				if(args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof String))
					throw new Exception("expected string, string");
				
				algorithm = (String)args[0];
				
				String input = (String)args[1];
				
				// input is either <ciphertext> or <algorithm>:<parameters>:<ciphertext>
				
				if(input.indexOf(':') >= 0) {
					String paramAlgorithm = algorithm;
					
					int i = input.indexOf(':');
					paramAlgorithm = input.substring(0, i);
					input = input.substring(i + 1);
					
					i = input.indexOf(':');
					if(i < 0)
						throw new Exception("malformed input");
					ciphertext = decodeBase64(input.substring(i + 1));
					input = input.substring(0, i);
					
					params = AlgorithmParameters.getInstance(paramAlgorithm);
					params.init(decodeBase64(input));
				} else {
					params = null;
					ciphertext = decodeBase64(input);
				}
				
				try {
					cipher = Cipher.getInstance(algorithm);
				} catch(NoSuchAlgorithmException e) {
					throw new Exception("unknown algorithm: "+algorithm);
				} catch(NoSuchPaddingException e) {
					throw new Exception("unknown padding: "+algorithm);
				}
				
				try {
					cipher.init(Cipher.DECRYPT_MODE, key, params);
				} catch(InvalidAlgorithmParameterException e) {
					throw new Exception("invalid algorithm parameter: "+e.getMessage());
				} catch(InvalidKeyException e) {
					throw new Exception("invalid key for algorithm: "+e.getMessage());
				}
				
				try {
					plaintext = cipher.doFinal(ciphertext);
				} catch(BadPaddingException e) {
					throw new Exception("padding error: "+e.getMessage());
				} catch(IllegalBlockSizeException e) {
					throw new Exception("illegal block size: "+e.getMessage());
				}
				
				return new Object[] {new String(plaintext, "UTF-8")};
				
			case 2:
				// encode() -> string encoded
				return new Object[] {key.getFormat() + ":" + encodeBase64(key.getEncoded())};
			}
			return null;
		}

		public String[] getMethodNames() {
			return new String[] {
				"encrypt",
				"decrypt",
				"encode",
				//"wrapKey",
				//"unwrapKey"
			};
		}
		
	}

	@Override
	public Object[] callMethod(IComputerAccess arg0, ILuaContext ctx, int arg1, Object[] args) throws Exception {
		switch(arg1) {
		case 0:
			
			// generateKeyPair(string algorithm, int keysize) -> key publicKey, key privateKey
			
			if(args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof Number))
				throw new Exception("Expected string, number");
			
			String algorithm = (String)args[0];
			int keySize = (int)(double)(Double)args[1];
			
			if(keySize > MAX_ASYMMETRIC_KEY_SIZE)
				throw new Exception("max key size is "+MAX_ASYMMETRIC_KEY_SIZE);
			
			KeyPairGenerator kpg;
			try {
				kpg = KeyPairGenerator.getInstance((String)algorithm);
			} catch(NoSuchAlgorithmException e) {
				throw new Exception("unknown algorithm: "+algorithm);
			}
			
			try {
				kpg.initialize(keySize);
			} catch(InvalidParameterException e) {
				throw new Exception("invalid key size: "+keySize);
			}
			
			KeyPair kp = kpg.generateKeyPair();
			return new Object[] {new LuaKey(kp.getPublic()), new LuaKey(kp.getPrivate())};
			
		case 1:
			// getCipherBlockSize(string algorithm) -> int blocksize
			if(args.length < 1 || !(args[0] instanceof String))
				throw new Exception("expected string");
			
			algorithm = (String)args[0];
			
			try {
				return new Object[] {Cipher.getInstance(algorithm).getBlockSize()};
			} catch(NoSuchAlgorithmException e) {
				throw new Exception("unknown algorithm: "+algorithm);
			}
			
		case 2:
			// generateSymmetricKey(string algorithm, int keysize) -> key
			
			if(args.length < 1 || !(args[0] instanceof String))
				throw new Exception("Expected string, [number]");
			if(args.length >= 2 && args[1] != null && !(args[1] instanceof Double))
				throw new Exception("Expected string, [number]");
			
			algorithm = (String)args[0];
			keySize = (args.length < 2 || args[1] == null) ? 0 : (int)(double)(Double)args[1];
			
			KeyGenerator kg;
			try {
				kg = KeyGenerator.getInstance((String)algorithm);
			} catch(NoSuchAlgorithmException e) {
				throw new Exception("unknown algorithm: "+algorithm);
			}
			
			try {
				if(keySize == 0)
					kg.init(new SecureRandom());
				else
					kg.init(keySize);
			} catch(InvalidParameterException e) {
				throw new Exception("invalid key size: "+keySize);
			}
			
			SecretKey sk = kg.generateKey();
			return new Object[] {new LuaKey(sk)};
			
		case 3:
			// decodeKey(string algorithm, string encoded-key) -> key
			if(args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof String))
				throw new Exception("Expected string, string");
			
			algorithm = (String)args[0];
			
			String str = (String)args[1];
			int i = str.indexOf(':');
			if(i < 0)
				throw new Exception("invalid encoded key");
			
			String fmt = str.substring(0, i);
			byte[] encKey = decodeBase64(str.substring(i+1));
			
			if(fmt.equals("RAW")) {
				return new Object[] {new LuaKey(new SecretKeySpec(encKey, algorithm))};
				
			} else if(fmt.equals("X.509")) {
				KeySpec keySpec = new X509EncodedKeySpec(encKey);
				
				KeyFactory kf;
				try {
					kf = KeyFactory.getInstance(algorithm);
				} catch(NoSuchAlgorithmException e) {
					throw new Exception("unknown algorithm: "+algorithm);
				}
				
				try {
					return new Object[] {new LuaKey(kf.generatePublic(keySpec))};
				} catch(InvalidKeySpecException e) {
					throw new Exception("invalid encoded key: "+e.getMessage());
				}
				
				
			} else if(fmt.equals("PKCS#8")) {
				KeySpec keySpec = new PKCS8EncodedKeySpec(encKey);
				
				KeyFactory kf;
				try {
					kf = KeyFactory.getInstance(algorithm);
				} catch(NoSuchAlgorithmException e) {
					throw new Exception("unknown algorithm: "+algorithm);
				}
				
				try {
					return new Object[] {new LuaKey(kf.generatePrivate(keySpec))};
				} catch(InvalidKeySpecException e) {
					throw new Exception("invalid encoded key: "+e.getMessage());
				}
				
			} else {
				throw new Exception("unknown key format "+fmt);
			}
			
		case 4:
			// hash(string algorithm, string input) -> string hex-hash
			if(args.length < 2 || !(args[0] instanceof String) || !(args[1] instanceof String))
				throw new Exception("expected string, string");
			
			algorithm = (String)args[0];
			byte[] input = ((String)args[1]).getBytes("UTF-8");
			
			MessageDigest d;
			try {
				d = MessageDigest.getInstance(algorithm);
			} catch(NoSuchAlgorithmException e) {
				throw new Exception("unknown algorithm: "+algorithm);
			}
			
			return new Object[] {bytesToHex(d.digest(input))};
		}
		
		return null;
	}

	public String[] getMethodNames() {
		return new String[] {
			"generateKeyPair",
			"getCipherBlockSize",
			"generateSymmetricKey",
			"decodeKey",
			"hash"
		};
	}

	@Override
	public String getType() {
		return "cryptographic accelerator";
	}

}
