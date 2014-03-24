package mods.immibis.ccperiphs.forth;

public class ForthBaseDictionary extends JavaDictionary {
	public ForthBaseDictionary() {
		addWord(1, "WORDS", new IJavaWord() {
			@Override
			public void execute(ForthContext context) {
				String s = "Forth:";
				for(String w : context.forth_dict.keySet())
					s += " " + w;
				context.writeWrapped(s);
				s = "Java:";
				for(String w : context.java_dict.getAllWords())
					s += " " + w;
				context.writeWrapped(s);
				s = "Base: ; :";
				for(String w : getAllWords())
					s += " " + w;
				context.writeWrapped(s);
			}
		});
		addWord(2, "PAGE", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.write("\n\n\n\n\n\n\n\n\n\n");
		}});
		addWord(3, ".", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.write(context.pop()+" ");
		}});
		addWord(4, "(lit)", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push((int)context.nextCode());
		}});
		addWord(5, "(lit2)", new IJavaWord() {@Override public void execute(ForthContext context) {
			short low = context.nextCode();
			short high = context.nextCode();
			context.push((low & 0xFFFF) + ((high & 0xFFFF) << 16));
		}});
		addWord(6, "RETURN", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.doReturn();
		}});
		addWord(7, "CR", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.write("\n");
		}});
		addWord(8, "DUP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object o = context.pop();
			context.push(o);
			context.push(o);
		}});
		addWord(9, "?DUP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object o = context.pop();
			context.push(o);
			if(o != null && (!(o instanceof Number) || !((Number)o).equals(0)))
				context.push(o);
		}});
		addWord(10, "2DUP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			context.push(b);
			context.push(a);
			context.push(b);
			context.push(a);
		}});
		addWord(11, "DROP", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.pop();
		}});
		addWord(12, "2DROP", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.pop();
			context.pop();
		}});
		addWord(13, "SWAP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			context.push(a);
			context.push(b);
		}});
		addWord(14, "2SWAP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			Object c = context.pop();
			Object d = context.pop();
			context.push(b);
			context.push(a);
			context.push(d);
			context.push(c);
		}});
		/*addWord(15, "PICK", new IJavaWord() {@Override public void execute(ForthContext context) {
			
		}});*/
		addWord(16, "OVER", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			context.push(b);
			context.push(a);
			context.push(b);
		}});
		addWord(17, "2OVER", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			Object c = context.pop();
			Object d = context.pop();
			context.push(d);
			context.push(c);
			context.push(b);
			context.push(a);
			context.push(d);
			context.push(c);
		}});
		addWord(18, "ROT", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			Object c = context.pop();
			context.push(b);
			context.push(a);
			context.push(c);
		}});
		addWord(19, "-ROT", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			Object c = context.pop();
			context.push(a);
			context.push(c);
			context.push(b);
		}});
		addWord(20, "NIP", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			context.pop();
			context.push(a);
		}});
		addWord(21, "TUCK", new IJavaWord() {@Override public void execute(ForthContext context) {
			Object a = context.pop();
			Object b = context.pop();
			context.push(a);
			context.push(b);
			context.push(a);
		}});
		addWord(22, "DO", new IJavaWord() {@Override public void execute(ForthContext context) {
			int min = context.popInt();
			int max = context.popInt();
			
			int pc = context.rpop();
			context.rpush(min);
			context.rpush(max);
			context.rpush(pc);
			context.rpush(pc);
		}});
		addWord(24, "LOOP", new IJavaWord() {@Override public void execute(ForthContext context) {
			LOOP(context, 1);
		}});
		addWord(25, "+LOOP", new IJavaWord() {@Override public void execute(ForthContext context) {
			LOOP(context, context.popInt());
		}});
		/*addWord(26, "LEAVE", new IJavaWord() {@Override public void execute(ForthContext context) {
			
		}});*/
		addWord(27, "BEGIN", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( -- dest )
			context.push(context.HERE());
		}});
		addWord(28, "UNTIL", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( dest -- )
			// runtime ( f -- )
			context.compileWord("(!?branch)");
			context.appendCode((short)context.popInt());
		}});
		addWord(29, "WHILE", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( dest -- orig dest )
			// runtime ( f -- )
			int BEGIN = context.popInt();
			context.compileWord("(!?branch)");
			int WHILE = context.HERE();
			context.appendCode((short)0);
			context.push(WHILE);
			context.push(BEGIN);
		}});
		addWord(30, "REPEAT", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( orig dest -- )
			context.compileWord("(branch)");
			context.appendCode((short)context.popInt());
			context.writeCode(context.popInt(), context.HERE());
		}});
		addWord(31, "AGAIN", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( dest -- )
			context.compileWord("(branch)");
			context.appendCode((short)context.popInt());
		}});
		addWord(32, "UNLOOP", new IJavaWord() {@Override public void execute(ForthContext context) {
			UNLOOP(context);
		}});
		addWord(33, "IF", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( -- orig )
			// runtime: ( f -- )
			context.compileWord("(!?branch)");
			context.push(context.HERE());
			context.appendCode((short)0);
		}});
		addWord(34, "THEN", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( orig -- )
			context.writeCode(context.popInt(), context.HERE());
		}});
		addWord(35, "ELSE", new IImmediateWord() {@Override public void execute(ForthContext context) {
			// ( orig1 -- orig2 )
			int IF = context.popInt();
			
			context.compileWord("(branch)");
			context.push(context.HERE());
			context.appendCode((short)0);
			
			context.writeCode(IF, context.HERE());
		}});
		addWord(36, "0=", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() == 0 ? -1 : 0);
		}});
		addWord(37, "0<>", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() != 0 ? -1 : 0);
		}});
		addWord(38, "0<", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() < 0 ? -1 : 0);
		}});
		addWord(39, "0>", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() > 0 ? -1 : 0);
		}});
		addWord(40, "<>", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a != b ? -1 : 0);
		}});
		addWord(41, ">", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a > b ? -1 : 0);
		}});
		addWord(42, "<", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a < b ? -1 : 0);
		}});
		addWord(43, ">=", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a >= b ? -1 : 0);
		}});
		addWord(44, "<=", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a <= b ? -1 : 0);
		}});
		addWord(45, "=", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			//System.out.println(a+" "+b+" = at pc="+context.rpick(0));
			context.push(a == b ? -1 : 0);
		}});
		addWord(46, "+", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a + b);
		}});
		addWord(47, "-", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a - b);
		}});
		addWord(48, "/", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a / b);
		}});
		addWord(49, "MOD", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a % b);
		}});
		addWord(50, "1+", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() + 1);
		}});
		addWord(51, "1-", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() - 1);
		}});
		addWord(52, "NEGATE", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(-context.popInt());
		}});
		addWord(53, "MAX", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(Math.max(context.popInt(), context.popInt()));
		}});
		addWord(54, "MIN", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(Math.min(context.popInt(), context.popInt()));
		}});
		addWord(55, "AND", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() & context.popInt());
		}});
		addWord(56, "OR", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() | context.popInt());
		}});
		addWord(57, "XOR", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt() ^ context.popInt());
		}});
		addWord(58, "INVERT", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(~context.popInt());
		}});
		addWord(59, "TRUE", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(-1);
		}});
		addWord(60, "FALSE", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(0);
		}});
		addWord(61, "2*", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt()*2);
		}});
		addWord(62, "2/", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.popInt()/2);
		}});
		addWord(63, "I", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.rpick(3));
		}});
		addWord(64, "J", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.rpick(6));
		}});
		addWord(65, "\\", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.getTIB().nextLine();
		}});
		addWord(66, "(", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.getTIB().next("[^)]*)");
		}});
		addWord(67, "*", new IJavaWord() {@Override public void execute(ForthContext context) {
			int a = context.popInt();
			int b = context.popInt();
			context.push(a * b);
		}});
		addWord(68, "(branch)", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.jump(context.nextCode());
		}});
		addWord(69, "(?branch)", new IJavaWord() {@Override public void execute(ForthContext context) {
			if(context.popInt() != 0) {
				context.jump(context.nextCode());
			} else {
				context.nextCode();
			}
		}});
		addWord(70, "(!?branch)", new IJavaWord() {@Override public void execute(ForthContext context) {
			int val = context.popInt();
			//System.out.println(val+" (!?branch) at pc="+context.rpick(0));
			if(val == 0) {
				context.jump(context.nextCode());
			} else {
				context.nextCode();
			}
		}});
		addWord(71, "VARIABLE", new IImmediateWord() {@Override public void execute(ForthContext context) {
			context.createWordHere(context.readWord());
			context.compileWord("(lit)");
			context.appendCode((short)(context.HERE() + 2));
			context.compileWord("RETURN");
			context.appendCode((short)0);
			context.endDefinition();
		}});
		addWord(72, "@", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.push(context.readCode(context.popInt()));
		}});
		addWord(73, "!", new IJavaWord() {@Override public void execute(ForthContext context) {
			int addr = context.popInt();
			int val = context.popInt();
			context.writeCode(addr, (short)val);
		}});
		addWord(74, "TICK", new IJavaWord() {@Override public void execute(ForthContext context) {
			context.waitForNextTick();
		}});
	}
	
	private void UNLOOP(ForthContext f) {
		int pc = f.rpop();
		f.rpop();
		f.rpop();
		f.rpop();
		f.rpush(pc);
	}
	
	private void LOOP(ForthContext f, int step) {
		int max = f.rpick(2);
		int cur = f.rpick(3);
		cur += step;
		f.rset(3, cur);
		if((cur <= max && step < 0) || (cur >= max && step > 0)) {
			UNLOOP(f); // loop done
		} else {
			f.rset(0, f.rpick(1)); // jump back to after DO
		}
	}
}
