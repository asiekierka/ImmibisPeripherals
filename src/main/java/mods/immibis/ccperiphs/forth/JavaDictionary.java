package mods.immibis.ccperiphs.forth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JavaDictionary {
	private Map<Short, IJavaWord> words_by_id = new HashMap<Short, IJavaWord>();
	private Map<String, Short> ids_by_name = new HashMap<String, Short>();
	
	public Set<String> getAllWords() {
		return Collections.unmodifiableSet(ids_by_name.keySet());
	}
	
	public void addWord(int id_, String name, IJavaWord word) {
		if(id_ <= 0 || id_ >= 16384)
			throw new RuntimeException("word IDs must be between 1 and 16383 inclusive");
		
		short id = (short)id_;
		words_by_id.put(id, word);
		ids_by_name.put(name, id);
	}
	
	public short getId(String name) {
		Short i = ids_by_name.get(name);
		if(i == null)
			return 0;
		else
			return i;
	}
	
	public IJavaWord getWord(short id) {
		return words_by_id.get((Short)id); 
	}
}
