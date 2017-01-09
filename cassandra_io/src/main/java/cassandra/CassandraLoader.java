package cassandra;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

public class CassandraLoader {
	private final int threads;
	private final String serverIP;
	
	public CassandraLoader(int threads, String serverIP){
	    this.threads = threads;
	    this.serverIP = serverIP; 
	}
	
	public static class InsertCallback implements FutureCallback<ResultSet>
	{
		
		@Override
		public void onSuccess(ResultSet result)
		{
		}
		
		@Override
		public void onFailure(Throwable t)
		{
			throw new RuntimeException(t);
		}
	}
	
	public long insert(BufferedReader reader, String insertCQL) throws InterruptedException
	{
		String serverIP = "localhost";
		
		Cluster cluster = Cluster.builder()
		    	  .addContactPoints(serverIP)
		          .withSocketOptions(
		                  new SocketOptions()
		                          .setConnectTimeoutMillis(600000)
		                          .setReadTimeoutMillis(600000))
		    	  .build();
		
		Session session = cluster.newSession();
		
		ExecutorService executor = MoreExecutors.getExitingExecutorService(
						(ThreadPoolExecutor)Executors.newFixedThreadPool(threads));
		
		final PreparedStatement statement = session.prepare(insertCQL);
		
		String line;
		long registros = 0;
		try {
			reader.readLine();
			
			while ((line = reader.readLine()) != null)   {
				registros++;
				String fields[] = line.split("\t");
				
				try
				{
					String uf = fields[0];
					int cod_municipio = Integer.parseInt(fields[1]);
					String nome_municipio = fields[2];
					long nis_favorecido = Long.parseLong(fields[7]);
					String nome_favorecido = fields[8];
					String fonte = fields[9];
					double valor = Double.parseDouble(fields[10].replaceAll(",", ""));
					
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/uuuu");
					YearMonth date = YearMonth.parse(fields[11], formatter);
					Timestamp periodo = Timestamp.valueOf(date.atDay(1).atStartOfDay());	
								
					BoundStatement boundStatement = statement.bind(uf, cod_municipio, nome_municipio, 
											nis_favorecido, nome_favorecido, fonte, valor, periodo);
					
					ResultSetFuture future = session.executeAsync(boundStatement);
					Futures.addCallback(future, new InsertCallback(), executor);
				}catch (NumberFormatException e)
				{
					System.out.println("Erro na linha:\n" + line + "\n");
					System.out.println(e.getMessage());
					continue;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		session.close();
		cluster.close();
		
		return registros;
	}
	
	
	
	
}
