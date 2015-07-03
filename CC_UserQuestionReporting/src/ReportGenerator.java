import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.opencsv.CSVReader;

/** This class generates a csv file by reading in 2 inout files:
 * 			File with questions in json format (user ID, row ID, parent row ID, content, type, note)
 * 			File with user data in CSV format (user ID, year of birth, disease type, story, ....)
 * Output file has all questions sorted by user ID. FOr each user, questions are listed by threads
 * with all Q&A for a thread listed together, sorted by timestamp
 * @author vaishali
 *
 */
public class ReportGenerator {
	
	String usersFilename = "";
	String questionsFilename = "";
	String outputFilename = "";
	HashMap<String, String[]> usersMap = new HashMap<String, String[]>();
	HashMap<String, Row> idRowMap = new HashMap<String, Row>();
	HashMap<String, List<Row>> userThreadMap = new HashMap<String, List<Row>>();
	HashMap<String, List<String>> parentMap = new HashMap<String, List<String>>();
	
	public ReportGenerator(String usersFilename, String questionsFilename, String outputFilename) {
		this.usersFilename = usersFilename;
		this.questionsFilename = questionsFilename;
		this.outputFilename = outputFilename;
	}
	
	public void generate() {	
		BufferedReader br = null;
		CSVReader fr = null;
		String line = "";
		Gson gson = new Gson();
		
		try {
			// read users into map
			fr = new CSVReader(new FileReader(usersFilename));
			String[] fields = null;		    
			while ((fields = fr.readNext()) != null) {
				usersMap.put(fields[0], fields);
			}
			
			// read questions into various maps
			br = new BufferedReader(new FileReader(questionsFilename));
			while ((line = br.readLine()) != null) {
				Row r = gson.fromJson(line, Row.class);   
				
				idRowMap.put(r.getId(), r);
				
				if (r.getParent() == null || r.getParent().isEmpty()) {
					List<Row> userThreads = userThreadMap.get(r.getUser());
					if (userThreads == null) {
						userThreads = new ArrayList<Row>();
					}
					userThreads.add(r);
					userThreadMap.put(r.getUser(), userThreads);
				}
				
				if (r.getParent() != null && !r.getParent().isEmpty()) {
					List<String> children = parentMap.get(r.getParent());
					if (children == null) {
						children = new ArrayList<String>();
						parentMap.put(r.getParent(), children);
					}
					children.add(r.getId());
				}				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				fr.close();
				br.close();
			}
			catch(Exception e) { e.printStackTrace(); }
		}
		// generate output
		reportStats();
	}
	
	private void getNestedChildren(String parentId, List<Row> children) {
		List<String> nextLevelIds = parentMap.get(parentId);
		if (nextLevelIds == null || nextLevelIds.isEmpty()) return;
		
		List<Row> nextLevelRows = new ArrayList<Row>();
		for (String nextLevelId : nextLevelIds) {
			nextLevelRows.add(idRowMap.get(nextLevelId));
		}
		Collections.sort(nextLevelRows, new Row.TimeComparator());
		for (Row nextLevelRow : nextLevelRows) {
			children.add(nextLevelRow);
			getNestedChildren(nextLevelRow.getId(), children);
			nextLevelRow = null;
		}
	}
	
	private void reportStats() {
		HashMap<String, Integer> userMsgStats = new HashMap<String, Integer>();
		HashMap<String, Integer> userThreadStats = new HashMap<String, Integer>();
		HashMap<Integer, Integer> threadSizeStats = new HashMap<Integer, Integer>();				
		int threadCount = 0;
		int messageCount = 0;
		int threadsStartedByCC = 0;
		int threadsStartedByUser = 0;
		int notes = 0;
		int userThreadCount = 0;
		int userMsgCount = 0;
		int threadSize = 0;
		int threadSizeNumThreads = 0;
		FileWriter fw; 
		
		try {
			fw = new FileWriter(outputFilename);
			fw.write(HEADER);
			fw.write("\n");
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}

		List<String> userIds = new ArrayList<String>(userThreadMap.keySet());
		Collections.sort(userIds);
		for (String userId : userIds) {
			userThreadCount = 0;
			userMsgCount = 0;
			List<Row> threadRows = userThreadMap.get(userId);
			String userFields[] = usersMap.get(userId);
			for (Row thread : threadRows) {
				threadCount++;
				messageCount++;
				userMsgCount++;
				userThreadCount++;
				threadSize = 1;

				if (thread.getType().equalsIgnoreCase(Row.QUESTION)) threadsStartedByUser++;
				else if (thread.getType().equalsIgnoreCase(Row.MAIL)) threadsStartedByCC++;
				else notes++;
				
				printRow(fw, thread, userFields, true);
				
				List<Row> childRows = new ArrayList<Row>(); 
				getNestedChildren(thread.getId(), childRows);
				for (Row childRow : childRows) {
					messageCount++;
					userMsgCount++;
					threadSize++;
					printRow(fw, childRow, userFields, false);
				}
				
				if (threadSizeStats.get(threadSize) != null) 
					threadSizeNumThreads = threadSizeStats.get(threadSize) + 1;
				else 
					threadSizeNumThreads = 1;
				threadSizeStats.put(new Integer(threadSize),  threadSizeNumThreads);

			}
			userThreadStats.put(userId,  userThreadCount);
			userMsgStats.put(userId, userMsgCount);				
		}
		
		try {
			fw.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}

		printStats(userThreadMap.size(), messageCount, threadCount, threadsStartedByUser, 
				threadsStartedByCC, notes);
		printUserStats(userIds, userThreadStats, userMsgStats);
		printThreadStats(threadSizeStats);
	}
	
	private void printStats(int numUsers, int numMsgs, int numThreads, 
							int numUserThreads, int numCCThreads, int numNotes) {
		System.out.println("Number of unique users," + numUsers);
		System.out.println("Number of messages," + numMsgs);
		System.out.println("Number of threads," +  numThreads);
		System.out.println("Number of threads started by user," + numUserThreads);
		System.out.println("Number of threads started by CC," + numCCThreads);
		System.out.println("Number of notes," + numNotes);
	}
	
	private void printUserStats(List<String> userIds,
								HashMap<String, Integer> userThreadStats,
								HashMap<String, Integer> userMsgStats) {
		System.out.println("UserId,Number of threads,Number of messages");
		for (String userId : userIds) {
			System.out.println(userId + "," + userThreadStats.get(userId) + "," + userMsgStats.get(userId));
		}
	}
	
	private void printThreadStats(HashMap<Integer, Integer> threadSizeStats) {
		System.out.println("Thread size,Number of threads");
		for (Integer t : threadSizeStats.keySet()) {
			System.out.println(t + "," + threadSizeStats.get(t));
		}
	}

	private static final String HEADER = "User ID,New Thread,Type,Date,Subject,Content,Message ID,Parent message ID"
			+ ",PID,partner,disease,doctor,trial,type,created,dob_year,postcode,blood,rh,gender,race,stage,diagnosed,location,size,unit,status,locations,radiation,surgery,response,treatments,present,members,biomarkers,stories";
	private void printRow(FileWriter pw, Row r, String[] userFields, boolean threadHead) {
		// escape " in question content
		Document doc = Jsoup.parse(r.getContent()); 
		String content = doc.body().text().replace("\"", "\"\"");
		
		try {
			pw.write("\"" + r.getUser() + 
					"\",\"" + (threadHead ? "Y" : " ") + 
					"\",\"" + r.getType() + 
					"\",\"" + r.getFormattedDate() + 
					"\",\"" + ((r.getSubject() == null) ? " " : r.getSubject()) + 
					"\",\"" + content + 
					"\",\"" + r.getId() + 
					"\",\"" + ((r.getParent() == null) ? " " : r.getParent()) + 
					"\"");
			if (userFields != null) {
				for (String field : userFields) {
					pw.write(",\"" + field + "\"");							
				}
			}
			pw.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ReportGenerator r = new ReportGenerator(args[0], args[1], args[2]);
		r.generate();
	}
}