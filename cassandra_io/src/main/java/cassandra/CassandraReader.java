package cassandra;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.kenai.jffi.Array;

public class CassandraReader 
{	
	static class QueryTimes
	{
		public String query;
		public ArrayList<Double> times = new ArrayList<Double>();
	}
		
	public double runQueries(Session session, List<String> queries) throws Exception
	{   
		List<Future<ResultSet>> futures = new ArrayList<>();
    	long start = System.nanoTime();
    	
		for(String query : queries)
		{
			futures.add(session.executeAsync(query));
		}
		
		// Aguarda todas as leituras serem realizadas
		while(futures.size() > 0) {
			Future<ResultSet> future = futures.remove(0);
			
			try {
				future.get();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
    	long elapsed = System.nanoTime() - start;
    	double seconds = (double)elapsed / 1000000000.0;

		return seconds;
	}

	public void read(Session session, int n, String clusterName) throws Exception
	{
		List<String> queries = new ArrayList<>();
		List<Double> times = new ArrayList<>();
		
		FileInputStream fstream = null;
		String[] queryFiles = new String[] { "queries_selection", "queries_sum", "queries_count" };
		for(String file : queryFiles)
		{
			fstream = new FileInputStream(file + ".txt");
		    	
			// Le as queries do arquivo e adiciona na lista de queries
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fstream));
			String line;
			
			while ((line = fileReader.readLine()) != null)   {
				if(line.isEmpty())
					continue;
				
				queries.add(line);
			}   	
			
			// Executa as queries contidas no arquivo times vezes
			// Adiciona os tempos na lista de queries
			for(int i = 0; i < n; i++)
			{
				double elapsed = runQueries(session, queries);
				times.add(elapsed);
			}
			
			FileWriter file = new FileWriter(clusterName + "_" + file + "_out.csv", true);  
			PrintWriter writer = new PrintWriter(file);
			
			for(Double time : times)
			{   
				writer.print(time.toString());
				writer.println();
			}
			
			writer.close();
		}
		
		

	}
}
