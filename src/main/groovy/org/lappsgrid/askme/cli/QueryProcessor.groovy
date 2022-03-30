package org.lappsgrid.askme.cli

import groovy.json.*
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.api.Status
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.askme.core.model.Token
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Serializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

/**
 *
 */
class QueryProcessor{

	// Set this to nfcorpus or trec-covid
//    static final String DOMAIN = "trec-covid"
	private String domain = "";

	public String ME = new String("cli.mailbox");

	public Configuration config = new Configuration()

	public PostOffice po = new PostOffice(config.EXCHANGE,config.HOST)
	
	Object lock;
		
	
	public QueryProcessor(String dom, loc) {
		System.out.println("domain is " + dom);
		this.domain = dom;
		
		this.lock = loc;
		//Thread t = new Thread(this);
		//t.start();
	}
	

	MailBox box
	//StopWords stopwords = new StopWords()
	Logger logger = LoggerFactory.getLogger("askme")
	String ID

	Map getDefaultParams() {
		return ["title-checkbox-1" : "1",
					  "title-weight-1" : "1.0",
					  "title-checkbox-2" : "2",
					  "title-weight-2" : "1.0",
					  "title-checkbox-3" : "3",
					  "title-weight-3" : "1.0",
					  "title-checkbox-4" : "4",
					  "title-weight-4" : "1.0",
					  "title-checkbox-5" : "5",
					  "title-weight-5" : "1.0",
					  "title-checkbox-6" : "6",
					  "title-weight-6" : "1.0",
					  "title-checkbox-7" : "7",
					  "title-weight-7" : "1.0",
					  "title-weight-x" : "0.9",
					  "abstract-checkbox-1" : "1",
					  "abstract-weight-1" : "1.0",
					  "abstract-checkbox-2" : "2",
					  "abstract-weight-2" : "1.0",
					  "abstract-checkbox-3" : "3",
					  "abstract-weight-3" : "1.0",
					  "abstract-checkbox-4" : "4",
					  "abstract-weight-4" : "1.0",
					  "abstract-checkbox-5" : "5",
					  "abstract-weight-5" : "1.0",
					  "abstract-checkbox-6" : "6",
					  "abstract-weight-6" : "1.0",
					  "abstract-checkbox-7" : "7",
					  "abstract-weight-7" : "1.0",
					  "abstract-weight-x" : "1.1",
					  "domain" : this.domain]

	}
	
	
	public void submitQuery(String id, String question, int limit) {
		
		Map params = getDefaultParams() 
	
		AskmeMessage message = new AskmeMessage()
		message.command = "$id"
		Packet packet = new Packet()
		packet.status = Status.OK
		packet.core = this.domain;
		packet.query = new Query(question, limit)
		message.setBody(packet)
		message.route(config.QUERY_MBOX)
		message.route(config.ELASTIC_MBOX)
		message.route(config.RANKING_MBOX)
		message.route(ME)
		message.setParameters(params)
		try {
			//queryStartTime = System.currentTimeMillis();
			po.send(message)
		}
		finally {
			synchronized (lock) {
				lock.wait()
			}
		}
	}

	List<String> removeStopWords(Iterable<Token> tokens) {
		List<String> result = []
		tokens.each { Token token ->
			//if (!stopwords.contains(token.word)) {
			//	result << token.word
			//}
		}
		return result
	}

	double similarity(Iterable<String> left, Iterable<String> right) {
		HashMap<String, int[]> map = new HashMap<String, int[]>();
		left.each { String token ->
			String t = token.toLowerCase();
			if (!map.containsKey(t)) {
				map.put(t, new int[2]);
			}
			map.get(t)[0]++;
		}
		right.each { String token ->
			String t = token.toLowerCase();
			if (!map.containsKey(t)) {
				map.put(t, new int[2]);
			}
			map.get(t)[1]++;
		}
		double dot = 0;
		double norma = 0;
		double normb = 0;
		for (Map.Entry<String, int[]> e : map.entrySet()) {
			int[] v = e.getValue();
			dot += v[0] * v[1];
			norma += v[0] * v[0];
			normb += v[1] * v[1];
		}
		norma = Math.sqrt(norma);
		normb = Math.sqrt(normb);
		if (dot == 0) {
			return 0;
		} else {
			return dot / (norma * normb);
		}
	}
}