package cassandra;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CassandraReader 
{	
	static class QueryTimes
	{
		public String query;
		public ArrayList<Double> times = new ArrayList<Double>();
	}
		
	public double runQuery(Session session, String query)
	{    	
    	long start = System.nanoTime();
    	
    	ResultSet results = null;
    	try{
    		results = session.execute(query);
    	}catch(Exception e)
    	{
    		System.out.println("Erro na query: \n" + query);
    	}
    	
    	long elapsed = System.nanoTime() - start;
    	double seconds = (double)elapsed / 1000000000.0;
    	
    	for (Row row : results) {
    	  System.out.println(row.toString());
    	}
    	
		return seconds;
	}

	public void read(Session session, int times, String clusterName) throws IOException
	{
		ArrayList<QueryTimes> selectionTimes = new ArrayList<QueryTimes>();
		
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream("queries.txt");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		    	
    	// Le as queries do arquivo e adiciona na lista de queries
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(fstream));
		String line;
    	
    	while ((line = fileReader.readLine()) != null)   {
			if(line.isEmpty())
				continue;
			
			QueryTimes newQueryTime = new QueryTimes();
			
			newQueryTime.query = line;
			selectionTimes.add(newQueryTime);
		} 
    	
    	// Executa as queries contidas no arquivo times vezes
    	// Adiciona os tempos na lista de queries
    	for(int i = 0; i < times; i++)
    	{
    		for(QueryTimes query : selectionTimes)
    		{
    			double time = runQuery(session, query.query);
    			query.times.add(time);
    		}
    	}
    	
    	FileWriter file = new FileWriter(clusterName + "_out.csv", true);  
    	PrintWriter writer = new PrintWriter(file);
    	
    	for(QueryTimes query : selectionTimes)
    	{
    		writer.print(query.query + ';');
    		for(Double time : query.times)
    		{
    			writer.print(time.toString() + ';');
    		}
    		writer.println();
    	}
    	
    	writer.close();
	}
}
