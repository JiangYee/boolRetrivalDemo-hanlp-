package BooleanModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import BooleanModel.BoolRetrival_eg.FreqAndId;
import utilities.MyComparator;
import utilities.Tokenize;

public class BoolRetrivalModel {
//	private TreeMap<String, ArrayList<Integer>> invertedIndex; // 倒排索引
	private TreeMap<String, TreeMap<Integer,Integer>> invertedIndex; // 倒排索引
	private TreeMap<Integer,TreeMap<String, Integer> > docID_TFmap = new TreeMap<Integer,TreeMap<String, Integer>>();//每篇文档中的词频

	public BoolRetrivalModel() {
		// TODO Auto-generated constructor stub
		MyComparator comparator = new MyComparator();
		invertedIndex = new TreeMap<String, TreeMap<Integer,Integer>>(comparator); //TreeMap按字典顺序排序
	}

	public TreeMap<String, TreeMap<Integer,Integer>> getInvertedIndex() {
		return invertedIndex;
	}
	

	public TreeMap<Integer, TreeMap<String, Integer>> getDocID_TFmap() {
		return docID_TFmap;
	}

	/**
	 * 建立倒排索引
	 * 
	 * @param documents 所有处理好的文档
	 * 
	 */
	public void buildInvertedIndex(TreeMap<Integer, ArrayList<String>> documents) {
		// 遍历所有文档数据
		Iterator<Integer> docIDs = documents.keySet().iterator();
		Integer docID = null;// 文档ID 1
		ArrayList<String> doc = null; // 文档1
		while (docIDs.hasNext()) {
			docID = docIDs.next();
			doc = documents.get(docID);
			String term = null; // 文档1中的词
			int frequency = 0 ; //词频
			for (int i = 0; i < doc.size(); i++) {
				term = doc.get(i);
				if (!invertedIndex.containsKey(term)) {// 如果倒排索引里没有这个term
					TreeMap<Integer,Integer> posting = new TreeMap<Integer,Integer>(); // 一个词的倒排记录
					frequency = docID_TFmap.get(docID).get(term);
					posting.put(docID,frequency);// 将当前的文档ID加入posting
					invertedIndex.put(term, posting);

				} else {// 如果倒排索引里有这个term
					TreeMap<Integer,Integer> posting = invertedIndex.get(term);// 获取倒排索引里该term对应的posting
					if (!posting.containsKey(docID)) { // 判断该posting是否含有该文档ID，否则加入（去重操作）
						frequency = docID_TFmap.get(docID).get(term);
						posting.put(docID,frequency);// 将当前的文档ID加入posting
						invertedIndex.put(term, posting);// 更新倒排索引里面的值

					}
				}
			}
		}
	}
	
	public void buildDocID_TFmap(TreeMap<Integer, ArrayList<String>> documents) {
		// 遍历所有文档数据
		Iterator<Integer> docIDs = documents.keySet().iterator();
		Integer docID = null;// 文档ID 1
		ArrayList<String> doc = null; // 文档1
		while (docIDs.hasNext()) {
			docID = docIDs.next();
			doc = documents.get(docID);
			TreeMap<String, Integer> TFMap = new TreeMap<String,Integer>();
			String term = null; // 文档1中的词
			int frequency = 0 ; //某个词的词频
			for (int i = 0; i < doc.size(); i++) {
				term = doc.get(i);
				if(!TFMap.containsKey(term)) {
					frequency++;
					TFMap.put(term, frequency);
				}else {
					frequency = TFMap.get(term);
					frequency++;
					TFMap.put(term, frequency);
				}
			}
			docID_TFmap.put(docID, TFMap);
		}
	}

	
	/**
	 * 将建立的倒排索引存入本地文档中
	 */
	public void writeIndex() {
		try {
			//System.out.println(Thread.currentThread().getContextClassLoader().getResource("")+"results//inverstedIndex.txt");

			FileWriter fw = new FileWriter(new File("/Users/j_yee/eclipse-workspace/BR_hanlp/results//inverstedIndex.txt"));
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("terms\tposting lists");
			bw.newLine();
			Iterator<String> it = invertedIndex.keySet().iterator();
			String term = null;
			TreeMap<Integer,Integer> posting = null;
			while(it.hasNext()) {
				term = it.next();
				posting = invertedIndex.get(term);
				StringBuffer out = new StringBuffer();
				out.append(term+"--->[");
				for (Integer docID : posting.keySet()) { 
					int frequency = posting.get(docID);
					out.append(docID+")"+frequency+") ");
					} 
					out.append(']');
					bw.write(out.toString());
					bw.newLine();
			}
			bw.flush();
			fw.close();
			bw.close();
			System.out.println("倒排索引存储完毕");
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * 根据布尔检索表达式获取满足的文档ID （注：该布尔检索式为简单的表达式，不含括号等改变运算优先级的符号）
	 * @param queryTerms 按序存储的检索词
	 * @param operators 按序存储的逻辑表达式 
	 * @return ArrayList<Integer> results
	 */
	public ArrayList<Integer> boolRetrival(String[] queryTerms, String[] operators) {
		//将检索词全部转化为小写
		for (int i = 0; i < queryTerms.length; i++) {
			queryTerms[i] = Tokenize.tokenize(queryTerms[i]);
		}
		ArrayList<Integer> results = new ArrayList<Integer>();
		if(null==operators) { //如果只有一个检索词 (前端传递的operators可能为null)
			if (this.invertedIndex.containsKey(queryTerms[0])) {
				results.addAll(this.invertedIndex.get(queryTerms[0]).keySet());
				return results;
			}else {
				return null;
			}
			
		}else {
			if (this.invertedIndex.containsKey(queryTerms[0])) {//倒排索引含有第一个词
				results.addAll(this.invertedIndex.get(queryTerms[0]).keySet()); 
				for (int i = 0; i < operators.length; i++) {
					if (this.invertedIndex.containsKey(queryTerms[i + 1])) {// 如果倒排索引含有下一个词
						if ("and".equals(operators[i])) {
							results.retainAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 与（交集）：去除含有第一个词但不含有第二个词的文档ID
						} else if ("or" .equals(operators[i])) {
							results.addAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 或（并集）：加入含有第二个词的所有文档ID
							results = this.removeDuplicate(results); //去除重复值
						} else {
							results.removeAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 非：去除含有第二个词的所有文档ID
						}
					} else { // 如果倒排索引中不含有下一个词
						if("and".equals(operators[i])) { //第一个逻辑运算符为and 那么没有满足的文档，返回null
							return null;
						} 								//第一个逻辑运算符为 or 或not,可以接着查找，results不变为前一步的计算结果
					}
				}
				
			}else {//倒排索引不含第一个词
				if("and".equals(operators[0]) || "not".equals(operators[0])){
					return null;  //第一个逻辑运算符为 and 或not，则没有，满足的文档，返回null
				}else {  //第一个逻辑运算符为or
					results.addAll(this.invertedIndex.get(queryTerms[1]).keySet()); //以第二个词作为第一个词进行逻辑运算
					for (int i = 1; i < operators.length; i++) { //逻辑运算符也从第二个开始遍历，即i=1
						if (this.invertedIndex.containsKey(queryTerms[i + 1])) {// 如果倒排索引含有下一个词
							if ("and".equals(operators[i])) {
								results.retainAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 与（交集）：去除含有第一个词但不含有第二个词的文档ID
							} else if ("or" .equals(operators[i])) {
								results.addAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 或（并集）：加入含有第二个词的所有文档ID
								results = this.removeDuplicate(results);//去除重复值
							} else {
								results.removeAll(this.invertedIndex.get(queryTerms[i + 1]).keySet());// 非：去除含有第二个词的所有文档ID
							}
						} else { // 如果倒排索引中不含有下一个词
							if("and".equals(operators[i])) { //第一个逻辑运算符为and 那么没有满足的文档，返回null
								return null;
							} 								//第一个逻辑运算符为 or 或not,可以接着查找，results不变为前一步的计算结果
						}
					}
				}
			}

		}
		return results;

	}
	/**
	 * 去除ArrayList<Integer> list里面重复的值
	 * @param list 
	 * @return
	 */
	private ArrayList<Integer> removeDuplicate ( ArrayList<Integer> list ) {
        ArrayList<Integer> unique = new ArrayList<Integer>();
        for ( int i=0; i<list.size(); i++ ) {
            if ( !unique.contains(list.get(i)) ) unique.add(list.get(i));
        }
        
        return unique;
    }
	//放到工具类里面，将map<integer,integer>转换成map<string,integer>
	private ArrayList<String> intArrayListtoStr ( ArrayList<Integer> list ) {
        ArrayList<String> unique = new ArrayList<String>();
        for ( int i=0; i<list.size(); i++ ) {
          unique.add(list.get(i).toString());
        }
        
        return unique;
    }
	
}
