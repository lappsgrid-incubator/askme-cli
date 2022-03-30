package org.lappsgrid.askme.cli


import groovy.json.*
//import org.lappsgrid.askme.cli.Version
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
import org.lappsgrid.askme.core.CSVParser
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ArgGroup
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 */
@Command(name="java -jar service.jar", description = "%nAskME (almost anything) command line utility",
        sortOptions = false, versionProvider = VersionProvider)
class Application implements Runnable {


    @Option(names=["-c", "--core"], description = "the ElasticSearch index to run the queries against", required = false, defaultValue = 'covid19')
    String core = "covid19"

    @Option(names=["-q", "--queryfile"], description = "location of the query file", paramLabel = "<FILE>", required = true)
    File queryFile

    @Option(names=['-l', '--limit'], description = "return this many results per query", arity = "1", required = false, defaultValue = "-1")
    int limit=10

    @Option(names=['-h','--help'], description = 'show this help and exit', usageHelp = true, order = 100)
    boolean showHelp

    @Option(names = ["-v", "--version"], description = "show application version number", versionHelp = true, order = 99)
    boolean showVersion
	
	
	MailBox box;
	
	String ME = 'cli.mailbox';
	
	Configuration config = new Configuration();
	
	void runProgram(Object lock) {
		
		println("Limiting run to ${limit} results per query")

		if (queryFile == null) {
			println "No input directory specified."
			return
		}
				
		CSVParser parser = new CSVParser();
		List csvlines = parser.parse(queryFile);
		QueryProcessor qp = new QueryProcessor(core, lock);
		
		for(int i=0;i<csvlines.size();i++){
					
			String temp = csvlines.get(i);
			String [] query = parser.getQueryIDAndValue(temp);
			qp.submitQuery(query[0], query[1], limit);
		}
		
		System.out.println("Run complete.");
	}

    void run() {
		
		Map ID_doc_index = [:]
		
		Object lock = new Object();
		
		System.out.println("Application running and config.Exchange and config.Host are:" + config.EXCHANGE + " and " + config.HOST);
		
		box = new MailBox(config.EXCHANGE, ME, config.HOST) {
					
					
			int numResultsAvg=0;
			int largestResultsReturned=0;
					
			@Override
			void recv(String s){
				AskmeMessage message = Serializer.parse(s, AskmeMessage)
				Packet packet = message.getBody()
				System.out.println("Documents returned> " + packet.documents.size());
						
				this.numResultsAvg += packet.documents.size();
						
				if(packet.documents.size() > largestResultsReturned) {
					largestResultsReturned = packet.documents.size();
				}
						
				packet.documents.each { Document doc ->
							println "${message.command}\t${doc.id}\taskme score: ${doc.score}\telastic score: ${doc.nscore}" //"${doc.title.text}"
				}
						
				synchronized (lock) {
					lock.notifyAll()
				}
		
			}
					
			int getResultsAvg(){
				return numResultsAvg
			}
					
			int getlargestResultSet() {
				return largestResultsReturned
			}
		}
		
		runProgram(lock);
		
		box.close();
		
        System.exit(0)
    }
	

    void print() {
        println "Application Settings"
        println "Collection : ${core}"
    }

    static void main(String[] args) {
        new CommandLine(new Application()).execute(args)
    }

    static void _main(String[] args) {
        Application app = new Application()
//        new CommandLine(app).parse(args)
        CommandLine cli = new CommandLine(app)
        
		try {
            cli.parseArgs(args)
        }
        catch (Exception e) {
            println()
            println e.message
            println()
            cli.usage(System.out)
            println()
            return
        }
        if (app.usageHelp) {
            println()
            cli.usage(System.out)
            println()
            return
        }

        if (app.versionHelp) {
            println "\nLappsgrid AskME Command Line Utility v" + ""
            println "Copyright 2022 The Lanugage Applications Grid\n"
            return
        }
    }
}
