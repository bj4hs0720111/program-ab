
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/
import org.alicebot.ab.*;
import org.alicebot.ab.utils.IOUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class Main {
    public static void main (String[] args) {
        MagicStrings.root_path = System.getProperty("user.dir");
        System.out.println("Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        mainFunction(args);
    }
    public static void mainFunction (String[] args) {
        String botName = "super";
        String action = "chat";
        System.out.println(MagicStrings.programNameVersion);
        for (String s : args) {
            System.out.println(s);
            String[] splitArg = s.split("=");
            if (splitArg.length >= 2) {
                String option = splitArg[0];
                String value = splitArg[1];
                if (option.equals("bot")) botName = value;
                if (option.equals("action")) action = value;
                if (option.equals("trace") && value.equals("true")) MagicBooleans.trace_mode = true;
                else MagicBooleans.trace_mode = false;
            }
        }
        System.out.println("trace mode = "+MagicBooleans.trace_mode);
        Graphmaster.enableShortCuts = true;
        Timer timer = new Timer();
        Bot bot = new Bot(botName, MagicStrings.root_path, action); //
        //bot.preProcessor.normalizeFile("c:/ab/log1.txt", "c:/ab/data/lognormal.txt");
        if (bot.brain.getCategories().size() < 100) bot.brain.printgraph();
        if (action.equals("chat")) testChat(bot, MagicBooleans.trace_mode);
        else if (action.equals("test")) testSuite(bot, MagicStrings.root_path+"/data/find.txt");
        else if (action.equals("ab")) testAB(bot);
        else if (action.equals("aiml2csv") || action.equals("csv2aiml")) convert(bot, action);
        else if (action.equals("abwq")) AB.abwq(bot);
    }
    public static void convert(Bot bot, String action) {
        if (action.equals("aiml2csv")) bot.writeAIMLIFFiles();
        else if (action.equals("csv2aiml")) bot.writeAIMLFiles();
    }
    public static void testAB (Bot bot) {
        MagicBooleans.trace_mode = true;
        AB.ab(bot);
        AB.terminalInteraction(bot) ;
    }
    public static void testShortCuts () {
        //testChat(new Bot("alice"));
        //Graphmaster.enableShortCuts = false;
        //Bot bot = new Bot("alice");
        //bot.brain.printgraph();
        //bot.brain.nodeStats();
        //Graphmaster.enableShortCuts = true;
        //bot = new Bot("alice");
        //bot.brain.printgraph();
        //bot.brain.nodeStats();
    }
    public static void testChat (Bot bot, boolean traceMode) {
        Chat chatSession = new Chat(bot);
//        bot.preProcessor.normalizeFile("c:/ab/bots/super/aiml/thats.txt", "c:/ab/bots/super/aiml/normalthats.txt");
        bot.brain.nodeStats();
        MagicBooleans.trace_mode = traceMode;
        String textLine="";
        while (true) {
            System.out.print("Human: ");
			textLine = IOUtils.readInputTextLine();
            if (textLine == null || textLine.length() < 1)  textLine = MagicStrings.null_input;
            if (textLine.equals("q")) System.exit(0);
            else if (textLine.equals("wq")) {
                bot.writeQuit();
                System.exit(0);
            }
            else if (textLine.equals("ab")) testAB(bot);
            else {
                String request = textLine;
                if (MagicBooleans.trace_mode) System.out.println("STATE="+request+":THAT="+chatSession.thatHistory.get(0).get(0)+":TOPIC="+chatSession.predicates.get("topic"));
                String response = chatSession.multisentenceRespond(request);
                while (response.contains("&lt;")) response = response.replace("&lt;","<");
                while (response.contains("&gt;")) response = response.replace("&gt;",">");
                System.out.println("Robot: "+response);

            }

        }
    }
    public static void testBotChat () {
        Bot bot = new Bot("alice");
        System.out.println(bot.brain.upgradeCnt+" brain upgrades");
        bot.brain.nodeStats();
        //bot.brain.printgraph();
        Chat chatSession = new Chat(bot);
        String request = "Hello.  How are you?  What is your name?  Tell me about yourself.";
        String response = chatSession.multisentenceRespond(request);
        System.out.println("Human: "+request);
        System.out.println("Robot: "+response);
    }
    public static void testSuite (Bot bot, String filename) {
        try{
            AB.passed.readAIMLSet(bot);
            AB.testSet.readAIMLSet(bot);
            System.out.println("Passed "+AB.passed.size()+" samples.");
            String textLine="";
            Chat chatSession = new Chat(bot);
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            HashSet<String> samples = new HashSet<String>();
            while ((strLine = br.readLine())!= null)   {
                samples.add(strLine);
            }
            ArrayList<String> sampleArray = new ArrayList<String>(samples);
            Collections.sort(sampleArray);
            for (String request : sampleArray) {
                if (request.startsWith("Human: ")) request = request.substring("Human: ".length(), request.length());
                Category c = new Category(0, bot.preProcessor.normalize(request), "*", "*", MagicStrings.blank_template, MagicStrings.null_aiml_file);
                if (AB.passed.contains(request)) System.out.println("--> Already passed "+request);
                else if (!bot.deletedGraph.existsCategory(c) && !AB.passed.contains(request)) {
                    String response = chatSession.multisentenceRespond(request);
                    System.out.println(count+". Human: "+request);
                    System.out.println(count+". Robot: "+response);
					textLine = IOUtils.readInputTextLine();
                    AB.terminalInteractionStep(bot, request, textLine, c);
                    count += 1;
                }
            }
            //Close the input stream
            br.close();
        } catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

}
