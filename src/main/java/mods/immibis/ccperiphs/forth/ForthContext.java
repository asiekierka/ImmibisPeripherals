package mods.immibis.ccperiphs.forth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;

public class ForthContext {
	
	private static final int INITIAL_CODESIZE = 64;
	private static final int MAX_CODESIZE = 32767;
	
	private final int INSTRS_PER_TICK;
	private final int MAX_SAVED_INSTRS;
	
	Scanner tib;
	public IOutputDevice out;
	private short[] code;
	private short codesize;
	JavaDictionary java_dict;
	Map<String, Short> forth_dict;
	
	private Stack<Object> stack = new Stack<Object>();
	private int[] rstack = new int[64];
	private int rstack_pos;
	
	private boolean halted = false;
	private boolean fatal = false;
	
	public void error(String s) {
		if(!halted)
			out.write(s+"\n");
		halted = true;
	}
	
	public void doReturn() {
		rpop();
	}
	
	public int rpop() {
		if(rstack_pos == -1) {
			error("Return stack empty");
			return 0;
		} else {
			return rstack[rstack_pos--];
		}
	}
	
	public int rpick(int index) {
		if(rstack_pos < index) {
			error("Return stack empty");
			return 0;
		} else {
			return rstack[rstack_pos-index];
		}
	}
	
	public void rset(int index, int val) {
		if(rstack_pos < index) {
			error("Return stack empty");
		} else {
			rstack[rstack_pos-index] = val;
		}
	}
	
	public int popInt() {
		Object o = pop();
		if(o instanceof Number)
			return ((Number) o).intValue();
		error("Expected number, got " + o /*== null ? "null" : o.getClass().getName()*/);
		return 0;
	}
	
	public Number popNumber() {
		Object o = pop();
		if(o instanceof Number)
			return ((Number) o);
		error("Expected number, got " + o/* == null ? "null" : o.getClass().getName()*/);
		return null;
	}
	
	public Scanner getTIB() {
		return tib;
	}
	
	public short nextCode() {
		if(rstack_pos == -1) {
			if(immed.isEmpty()) {
				error("nextCode() called outside of word");
				//new Exception("nextCode() called outside of word - possible VM bug.").printStackTrace();
				//disassemble();
				return 0;
			} else {
				return immed.poll();
			}
		}
		if(rstack[rstack_pos] >= codesize) {
			error("End of code reached (at "+rstack[rstack_pos]+")");
			halted = true;
			return 0;
		}
		return code[rstack[rstack_pos]++];
	}
	
	private void disassemble() {
		Map<Integer, String> words = new HashMap<Integer, String>();
		for(String s : BASE_DICT.getAllWords()) words.put(0xC000 | (BASE_DICT.getId(s)), s);
		for(String s : java_dict.getAllWords()) words.put(0x8000 | (java_dict.getId(s)), s);
		for(String s : forth_dict.keySet()) words.put((int)(short)forth_dict.get(s), s);
		for(int k = 0; k < codesize; k++) {
			int c = code[k] & 0xFFFF;
			if(words.containsKey(c))
				System.out.println(k+": "+c+" ("+words.get(c)+")");
			else
				System.out.println(k+": "+c);
		}
	}

	public void doCall(short addr) {
		rpush(addr);
	}
	public void rpush(int val) {
		if(rstack_pos == rstack.length - 1) {
			error("Return stack overflow");
			halted = true;
		} else {
			rstack[++rstack_pos] = val;
		}
	}
	
	public Object pop() {
		if(stack.isEmpty()) {
			error("Empty stack");
			halted = true;
			return null;
		}
		return stack.pop();
	}
	public void push(Object o) {
		if(stack.size() > 256) {
			error("Stack overflow");
			halted = true;
			return;
		}
		stack.push(o);
	}
	
	public void write(String s) {
		out.write(s);
	}
	
	public void writeWrapped(String s) {
		final int WIDTH = 50;
		while(s.length() > WIDTH) {
			write(s.substring(0, WIDTH) + "\n");
			s = s.substring(WIDTH);
		}
		if(s.length() > 0)
			write(s + "\n");
	}
	
	// SAM forth pipe used 100 IPT, 10000 MSI
	// IP speaker uses 1000 IPT, 20000 MSI
	public ForthContext(IOutputDevice out, JavaDictionary java_dict, int INSTRS_PER_TICK, int MAX_SAVED_INSTRS) {
		this.tib = null;
		this.out = out;
		this.java_dict = java_dict;
		this.INSTRS_PER_TICK = INSTRS_PER_TICK;
		this.MAX_SAVED_INSTRS = MAX_SAVED_INSTRS;
		reset();
	}
	
	public String readWord() {
		if(tib.hasNext())
			return tib.next();
		return "";
	}
	
	private String defining = null;
	private Queue<Short> immed = new LinkedList<Short>();
	
	public void appendCode(short i) {
		if(defining != null) {
			if(codesize == code.length - 1) {
				if(codesize == MAX_CODESIZE - 1) {
					out.write("Out of memory\n");
					halted = true;
					fatal = true;
					return;
				}
				short[] newCode = new short[code.length*2];
				System.arraycopy(code, 0, newCode, 0, code.length);
				code = newCode;
			}
			code[codesize++] = i;
		} else {
			immed.add(i);
		}
	}
	
	public void compileWord(String name) {
		if(forth_dict.containsKey(name)) {
			appendCode(forth_dict.get(name));
		} else if(java_dict.getId(name) > 0) {
			short id = java_dict.getId(name);
			appendCode((short) (id | 0x8000));
		} else if(BASE_DICT.getId(name) > 0) {
			short id = BASE_DICT.getId(name);
			appendCode((short) (id | 0xC000));
		} else {
			error("Unknown word: " + name);
		}
	}
	
	public short HERE() {
		return codesize;
	}
	
	public void writeCode(int ptr, short val) {
		if(ptr < codesize && ptr >= 0)
			code[ptr] = val;
	}
	
	// In saved code, IDs are munged as follows:
	// 0xxxxxxxxxxxxxxx = Forth word
	// 10xxxxxxxxxxxxxx = java_dict word
	// 11xxxxxxxxxxxxxx = BASE_DICT word
	
	public void interpret(Scanner tib) {
		this.tib = tib;
		
		defining = null;
		immed.clear();
		
		while(tib.hasNext() && !fatal) {
			String wordName = readWord();
			//System.out.println("Interpret "+wordName);
			if(forth_dict.containsKey(wordName)) {
				appendCode(forth_dict.get(wordName));
			} else if(java_dict.getId(wordName) > 0) {
				short id = java_dict.getId(wordName);
				IJavaWord word = java_dict.getWord(id);
				if(word != null) {
					if(word instanceof IImmediateWord)
						word.execute(this);
					else
						appendCode((short) (id | 0x8000));
				} else {
					new Exception("java_dict.getWord("+id+") is null even though java_dict.getId(\""+wordName+"\") == "+id).printStackTrace();
				}
			} else if(BASE_DICT.getId(wordName) > 0) {
				short id = BASE_DICT.getId(wordName);
				IJavaWord word = BASE_DICT.getWord(id);
				if(word != null) {
					if(word instanceof IImmediateWord)
						word.execute(this);
					else
						appendCode((short) (id | 0xC000));
				} else {
					new Exception("BASE_DICT.getWord("+id+") is null even though BASE_DICT.getId(\""+wordName+"\") == "+id).printStackTrace();
				}
			} else if(wordName.equals(":")) {
				String name = readWord();
				if(defining != null) {
					out.write(": found while compiling ("+name+" is inside "+defining+")\n");
					halted = true;
					fatal = true;
					return;
				}
				forth_dict.put(name, codesize);
				defining = name;
			} else if(wordName.equals(";")) {
				if(defining == null) {
					out.write("; found while not compiling a word\n");
					halted = true;
					fatal = true;
					return;
				}
				appendCode((short) (BASE_DICT.getId("RETURN") | 0xC000));
				defining = null;
			} else {
				try {
					int n = Integer.parseInt(wordName);
					if((n & 0xFFFF0000) == 0) {
						appendCode((short) (BASE_DICT.getId("(lit)") | 0xC000));
						appendCode((short) n);
					} else {
						appendCode((short) (BASE_DICT.getId("(lit2)") | 0xC000));
						appendCode((short) n);
						appendCode((short) (n >> 16));
					}
				} catch(NumberFormatException e) {
					out.write("Unknown word: " + wordName + "\n");
					halted = true;
				}
			}
		}
		if(defining != null) {
			out.write("Unclosed definition (of " + defining + ")");
			halted = true;
		}
		defining = null;
		
		//disassemble();
	}
	
	private int instrs;
	private boolean waiting;
	
	public void waitForNextTick() {
		waiting = true;
	}
	
	public void tick() {
		waiting = false;
		instrs = Math.min(instrs + INSTRS_PER_TICK, MAX_SAVED_INSTRS);
		while(!waiting && !halted && instrs > 0) {
			runInstr();
			instrs--;
		}
	}
	
	public void runInstr() {
		short wordId;
		if(rstack_pos != -1) {
			wordId = code[rstack[rstack_pos]++];
		} else {
			if(!immed.isEmpty()) {
				wordId = immed.poll();
			} else {
				waiting = true;
				return;
			}
		}
		//System.out.println("Running word " + wordId + " (at "+(rstack[rstack_pos] - 1)+") (codesize is "+codesize+")");
		if(wordId < 0) {
			IJavaWord word = ((wordId & 0xC000) == 0xC000 ? BASE_DICT : java_dict).getWord((short)(wordId & 0x3FFF));
			if(word == null) {
				error("Invalid word ID: " + wordId);
				halted = true;
				return;
			}
			word.execute(this);
		} else {
			doCall(wordId);
		}
	}
	
	public void halt() {
		halted = true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NBTTagCompound save() {
		NBTTagCompound tag = new NBTTagCompound();
		
		tag.setShort("codesize", codesize);
		tag.setInteger("instrs", instrs);
		tag.setBoolean("halted", halted);
		tag.setBoolean("fatal", fatal);
		
		int[] list = new int[this.immed.size()];
		int listP = 0;
		for(short i : this.immed)
			list[listP++] = (int) i;
		tag.setTag("immed", new NBTTagIntArray(list));
		
		tag.setTag("rstack", new NBTTagIntArray(rstack));
		
		list = new int[this.stack.size()];
		listP = 0;
		for(Integer i : (List<Integer>)(List)stack)
			list[listP++] = i;
		tag.setTag("stack", new NBTTagIntArray(list));
		
		NBTTagList tagList = new NBTTagList();
		for(Map.Entry<String, Short> e : forth_dict.entrySet()) {
			NBTTagCompound c = new NBTTagCompound();
			c.setString("k", e.getKey());
			c.setShort("v", e.getValue());
			tagList.appendTag(c);
		}
		tag.setTag("forth_dict", tagList);
		
		byte[] b = new byte[codesize*2];
		for(int k = 0; k < codesize; k++) {
			b[k*2] = (byte)code[k];
			b[k*2+1] = (byte)(code[k] >> 8);
		}
		tag.setByteArray("code", b);
		
		return tag;
	}
	public void load(NBTTagCompound tag) {
		codesize = tag.getShort("codesize");
		instrs = tag.getInteger("instrs");
		halted = tag.getBoolean("halted");
		fatal = tag.getBoolean("fatal");
		
		int[] array = tag.getIntArray("immed");
		for(int k = 0; k < array.length; k++)
			immed.add((short) array[k]);
		
		array = tag.getIntArray("rstack");
		rstack_pos = Math.min(array.length - 1, rstack.length - 1);
		for(int k = 0; k <= rstack_pos; k++)
			rstack[k] = array[k];
		
		stack.clear();
		array = tag.getIntArray("stack");
		for(int k = 0; k < array.length; k++) {
			//NBTTagCompound stackTag = (NBTTagCompound)list.tagAt(k);
			stack.push(array[k]);
		}
		
		forth_dict.clear();
		NBTTagList list = tag.getTagList("forth_dict", 10);
		for(int k = 0; k < list.tagCount(); k++) {
			NBTTagCompound e = (NBTTagCompound)list.getCompoundTagAt(k);
			forth_dict.put(e.getString("k"), e.getShort("v"));
		}
		
		code = new short[codesize];
		byte[] codeBytes = tag.getByteArray("code");
		for(int k = 0; k < codeBytes.length - 1 && k < codesize*2; k += 2) {
			code[k/2] = (short)((codeBytes[k] & 255) | (codeBytes[k+1] << 8));
		}
	}

	public void reset() {
		code = new short[INITIAL_CODESIZE];
		codesize = 0;
		forth_dict = new HashMap<String, Short>();
		rstack_pos = -1;
		halted = false;
		fatal = false;
		instrs = 0;
		waiting = false;
		stack.clear();
	}
	
	public void reboot() {
		rstack_pos = -1;
		halted = false;
		fatal = false;
		instrs = 0;
		waiting = false;
		stack.clear();
	}
	
	
	private static JavaDictionary BASE_DICT = new ForthBaseDictionary();

	public int getForthWord(String name) {
		if(forth_dict.containsKey(name))
			return forth_dict.get(name);
		return -1;
	}

	public void queue(int word) {
		if(immed.size() < 50) {
			//System.out.println("queue("+word+")");
			immed.add((short)word);
		}
	}

	public void jump(short target) {
		rpop();
		rpush(target);
	}

	public void createWordHere(String name) {
		forth_dict.put(name, codesize);
		defining = name;
	}

	public short readCode(int addr) {
		return (addr >= codesize || addr < 0 ? 0 : code[addr]);
	}

	public void endDefinition() {
		defining = null;
	}

	public void compileString(String tag) {
		for(int k = 0; k < tag.length(); k++)
			appendCode((short)tag.charAt(k));
		appendCode((short)0);
	}
	
	public String readCompiledString() {
		StringBuffer s = new StringBuffer();
		while(instrs > -100) {
			char c = (char)nextCode();
			if(c == 0)
				break;
			--instrs;
			s.append(c);
		}
		if(instrs <= -100)
			error("compiled string too long");
		return s.toString();
	}

	public void setMemoryContents(short[] mem) {
		if(mem.length > 32768)
			throw new IllegalArgumentException("length must be 32768 or less");
		code = mem;
		codesize = mem.length > 32767 ? 32767 : (short)mem.length;
		//System.out.println("setting codesize to "+codesize);
	}
}
