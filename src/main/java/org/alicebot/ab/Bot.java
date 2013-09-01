package org.alicebot.ab;
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
import java.io.*;
import java.util.*;

/**
 * Class representing the AIML bot
 */
public class Bot {
    public final Properties properties = new Properties();
    public final PreProcessor preProcessor;
    public final Graphmaster brain;
    public final Graphmaster inputGraph;
    public final Graphmaster learnfGraph;
    public final Graphmaster patternGraph;
    public final Graphmaster deletedGraph;
    public Graphmaster unfinishedGraph;
  //  public final ArrayList<Category> categories;
    public ArrayList<Category> suggestedCategories;
    public String name=MagicStrings.unknown_bot_name;
    public HashMap<String, AIMLSet> setMap = new HashMap<String, AIMLSet>();
    public HashMap<String, AIMLMap> mapMap = new HashMap<String, AIMLMap>();

    /**
     * Set all directory path variables for this bot
     *
     * @param root        root directory of Program AB
     * @param name        name of bot
     */
    public void setAllPaths (String root, String name) {
        MagicStrings.bot_path = root+"/bots";
        MagicStrings.bot_name_path = MagicStrings.bot_path+"/"+name;
        System.out.println("Name = "+name+" Path = "+MagicStrings.bot_name_path);
        MagicStrings.aiml_path = MagicStrings.bot_name_path+"/aiml";
        MagicStrings.aimlif_path = MagicStrings.bot_name_path+"/aimlif";
        MagicStrings.config_path = MagicStrings.bot_name_path+"/config";
        MagicStrings.log_path = MagicStrings.bot_name_path+"/logs";
        MagicStrings.sets_path = MagicStrings.bot_name_path+"/sets";
        MagicStrings.maps_path = MagicStrings.bot_name_path+"/maps";
        System.out.println(MagicStrings.root_path);
        System.out.println(MagicStrings.bot_path);
        System.out.println(MagicStrings.bot_name_path);
        System.out.println(MagicStrings.aiml_path);
        System.out.println(MagicStrings.aimlif_path);
        System.out.println(MagicStrings.config_path);
        System.out.println(MagicStrings.log_path);
        System.out.println(MagicStrings.sets_path);
        System.out.println(MagicStrings.maps_path);
    }

    /**
     * Constructor (default action, default path, default bot name)
     */
    public Bot() {
        this(MagicStrings.default_bot);
    }

    /**
     * Constructor (default action, default path)
     * @param name
     */
    public Bot(String name) {
        this(name, MagicStrings.root_path);
    }

    /**
     * Constructor (default action)
     *
     * @param name
     * @param path
     */
    public Bot(String name, String path) {
        this(name, path, "auto");
    }

    /**
     * Constructor
     *
     * @param name     name of bot
     * @param path     root path of Program AB
     * @param action   Program AB action
     */
    public Bot(String name, String path, String action) {
        this.name = name;
        setAllPaths(path, name);
        this.brain = new Graphmaster(this);
        this.inputGraph = new Graphmaster(this);
        this.learnfGraph = new Graphmaster(this);
        this.deletedGraph = new Graphmaster(this);
        this.patternGraph = new Graphmaster(this);
        this.unfinishedGraph = new Graphmaster(this);
      //  this.categories = new ArrayList<Category>();
        this.suggestedCategories = new ArrayList<Category>();
        preProcessor = new PreProcessor(this);
        addProperties();
        addAIMLSets();
        addAIMLMaps();
        AIMLSet number = new AIMLSet(MagicStrings.natural_number_set_name);
        setMap.put(MagicStrings.natural_number_set_name, number);
        AIMLMap successor = new AIMLMap(MagicStrings.map_successor);
        mapMap.put(MagicStrings.map_successor, successor);
        AIMLMap predecessor = new AIMLMap(MagicStrings.map_predecessor);
        mapMap.put(MagicStrings.map_predecessor, predecessor);
        //System.out.println("setMap = "+setMap);
        Date aimlDate = new Date(new File(MagicStrings.aiml_path).lastModified());
        Date aimlIFDate = new Date(new File(MagicStrings.aimlif_path).lastModified());
        System.out.println("AIML modified "+aimlDate+" AIMLIF modified "+aimlIFDate);
        readDeletedIFCategories();
        readUnfinishedIFCategories();
        MagicStrings.pannous_api_key = Utilities.getPannousAPIKey();
        MagicStrings.pannous_login = Utilities.getPannousLogin();
        if (action.equals("aiml2csv")) addCategoriesFromAIML();
        else if (action.equals("csv2aiml")) addCategoriesFromAIMLIF();
        else if (aimlDate.after(aimlIFDate)) {
            System.out.println("AIML modified after AIMLIF");
            addCategoriesFromAIML();
            writeAIMLIFFiles();
        }
        else {
            addCategoriesFromAIMLIF();
            if (brain.getCategories().size()==0) {
                System.out.println("No AIMLIF Files found.  Looking for AIML");
                addCategoriesFromAIML();
            }
        }
        System.out.println("--> Bot "+name+" "+brain.getCategories().size()+" completed "+deletedGraph.getCategories().size()+" deleted "+unfinishedGraph.getCategories().size()+" unfinished");
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file      name of AIML file
     * @param moreCategories    list of categories
     */
    void addMoreCategories (String file, ArrayList<Category> moreCategories) {
        if (file.contains(MagicStrings.deleted_aiml_file)) {
            for (Category c : moreCategories) {
                //System.out.println("Delete "+c.getPattern());
                deletedGraph.addCategory(c);
            }
        } else if (file.contains(MagicStrings.unfinished_aiml_file)) {
            for (Category c : moreCategories) {
                //System.out.println("Delete "+c.getPattern());
                if (brain.findNode(c) == null)
                unfinishedGraph.addCategory(c);
                else System.out.println("unfinished "+c.inputThatTopic()+" found in brain");
            }
        } else if (file.contains(MagicStrings.learnf_aiml_file) ) {
            System.out.println("Reading Learnf file");
            for (Category c : moreCategories) {
                brain.addCategory(c);
                learnfGraph.addCategory(c);
                patternGraph.addCategory(c);
            }
            //this.categories.addAll(moreCategories);
        } else {
            for (Category c : moreCategories) {
                //System.out.println("Brain size="+brain.root.size());
                //brain.printgraph();
                brain.addCategory(c);
                patternGraph.addCategory(c);
                //brain.printgraph();
            }
            //this.categories.addAll(moreCategories);
        }
    }

    /**
     * Load all brain categories from AIML directory
     */
    void addCategoriesFromAIML() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.aiml_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                System.out.println("Loading AIML files from "+MagicStrings.aiml_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".aiml") || file.endsWith(".AIML")) {
                            System.out.println(file);
                            try {
                                ArrayList<Category> moreCategories = AIMLProcessor.AIMLToCategories(MagicStrings.aiml_path, file);
                                addMoreCategories(file, moreCategories);
                            } catch (Exception iex) {
                                System.out.println("Problem loading " + file);
                                iex.printStackTrace();
                            }
                        }
                    }
                }
            }
            else System.out.println("addCategories: "+MagicStrings.aiml_path+" does not exist.");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
        System.out.println("Loaded " + brain.getCategories().size() + " categories in " + timer.elapsedTimeSecs() + " sec");
    }

    /**
     * load all brain categories from AIMLIF directory
     */
    void addCategoriesFromAIMLIF() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.aimlif_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                System.out.println("Loading AIML files from "+MagicStrings.aimlif_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(MagicStrings.aimlif_file_suffix) || file.endsWith(MagicStrings.aimlif_file_suffix.toUpperCase())) {
                            //System.out.println(file);
                            try {
                                ArrayList<Category> moreCategories = readIFCategories(MagicStrings.aimlif_path + "/" + file);
                                addMoreCategories(file, moreCategories);
                             //   MemStats.memStats();
                            } catch (Exception iex) {
                                System.out.println("Problem loading " + file);
                                iex.printStackTrace();
                            }
                        }
                    }
                }
            }
            else System.out.println("addCategories: "+MagicStrings.aimlif_path+" does not exist.");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
        System.out.println("Loaded " + brain.getCategories().size() + " categories in " + timer.elapsedTimeSecs() + " sec");
    }

    /**
     * read deleted categories from AIMLIF file
     */
    public void readDeletedIFCategories() {
        readCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
    }

    /**
     * read unfinished categories from AIMLIF file
     */
    public void readUnfinishedIFCategories() {
        readCertainIFCategories(unfinishedGraph, MagicStrings.unfinished_aiml_file);
    }

    /**
     * update unfinished categories removing any categories that have been finished
     */
    public void updateUnfinishedCategories () {
        ArrayList<Category> unfinished = unfinishedGraph.getCategories();
        unfinishedGraph = new Graphmaster(this);
        for (Category c : unfinished) {
            if (!brain.existsCategory(c)) unfinishedGraph.addCategory(c);
        }
    }

    /**
     * write all AIML and AIMLIF categories
     */
    public void writeQuit() {
        writeAIMLIFFiles();
        System.out.println("Wrote AIMLIF Files");
        writeAIMLFiles();
        System.out.println("Wrote AIML Files");
        writeDeletedIFCategories();
        updateUnfinishedCategories();
        writeUnfinishedIFCategories();

    }

    /**
     * read categories from specified AIMLIF file into specified graph
     *
     * @param graph   Graphmaster to store categories
     * @param fileName   file name of AIMLIF file
     */
    public void readCertainIFCategories(Graphmaster graph, String fileName) {
        File file = new File(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix);
        if (file.exists()) {
            try {
                ArrayList<Category> deletedCategories = readIFCategories(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix);
                for (Category d : deletedCategories) graph.addCategory(d);
                System.out.println("readCertainIFCategories "+graph.getCategories().size()+" categories from "+fileName+MagicStrings.aimlif_file_suffix);
            } catch (Exception iex) {
                System.out.println("Problem loading " + fileName);
                iex.printStackTrace();
            }
        }
        else System.out.println("No "+MagicStrings.deleted_aiml_file+MagicStrings.aimlif_file_suffix+" file found");
    }

    /**
     * write certain specified categories as AIMLIF files
     *
     * @param graph       the Graphmaster containing the categories to write
     * @param file        the destination AIMLIF file
     */
    public void writeCertainIFCategories(Graphmaster graph, String file) {
        if (MagicBooleans.trace_mode) System.out.println("writeCertainIFCaegories "+file+" size= "+graph.getCategories().size());
        writeIFCategories(graph.getCategories(), file+MagicStrings.aimlif_file_suffix);
        File dir = new File(MagicStrings.aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * write deleted categories to AIMLIF file
     */
    public void writeDeletedIFCategories() {
        writeCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
    }

    /**
     * write learned categories to AIMLIF file
     */
    public void writeLearnfIFCategories() {
        writeCertainIFCategories(learnfGraph, MagicStrings.learnf_aiml_file);
    }

    /**
     * write unfinished categories to AIMLIF file
     */
    public void writeUnfinishedIFCategories() {
        writeCertainIFCategories(unfinishedGraph, MagicStrings.unfinished_aiml_file);
    }

    /**
     * write categories to AIMLIF file
     *
     * @param cats           array list of categories
     * @param filename       AIMLIF filename
     */
    public void writeIFCategories (ArrayList<Category> cats, String filename)  {
        //System.out.println("writeIFCategories "+filename);
        BufferedWriter bw = null;
        File existsPath = new File(MagicStrings.aimlif_path);
        if (existsPath.exists())
        try {
            //Construct the bw object
            bw = new BufferedWriter(new FileWriter(MagicStrings.aimlif_path+"/"+filename)) ;
            for (Category category : cats) {
                bw.write(Category.categoryToIF(category));
                bw.newLine();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the bw
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Write all AIMLIF files from bot brain
     */
    public void writeAIMLIFFiles () {
        System.out.println("writeAIMLIFFiles");
        HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
        if (deletedGraph.getCategories().size() > 0) writeDeletedIFCategories();
        ArrayList<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {
            try {
                BufferedWriter bw;
                String fileName = c.getFilename();
                if (fileMap.containsKey(fileName)) bw = fileMap.get(fileName);
                else {
                    bw = new BufferedWriter(new FileWriter(MagicStrings.aimlif_path+"/"+fileName+MagicStrings.aimlif_file_suffix));
                    fileMap.put(fileName, bw);

                }
                bw.write(Category.categoryToIF(c));
                bw.newLine();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Set set = fileMap.keySet();
        for (Object aSet : set) {
            BufferedWriter bw = fileMap.get(aSet);
            //Close the bw
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }

        }
        File dir = new File(MagicStrings.aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public void writeAIMLFiles () {
        HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
        Category b = new Category(0, "BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        b = new Category(0, "DELEVLOPMENT ENVIRONMENT", "*", "*", MagicStrings.programNameVersion, "update.aiml");
        brain.addCategory(b);
        ArrayList<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {

            if (!c.getFilename().equals(MagicStrings.null_aiml_file))
            try {
                //System.out.println("Writing "+c.getCategoryNumber()+" "+c.inputThatTopic());
                BufferedWriter bw;
                String fileName = c.getFilename();
                if (fileMap.containsKey(fileName)) bw = fileMap.get(fileName);
                else {
                    String copyright = Utilities.getCopyright(this, fileName);
                    bw = new BufferedWriter(new FileWriter(MagicStrings.aiml_path+"/"+fileName));
                    fileMap.put(fileName, bw);
                    bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                            "<aiml>\n");
                    bw.write(copyright);
                     //bw.newLine();
                }
                bw.write(Category.categoryToAIML(c)+"\n");
                //bw.newLine();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        Set set = fileMap.keySet();
        for (Object aSet : set) {
            BufferedWriter bw = fileMap.get(aSet);
            //Close the bw
            try {
                if (bw != null) {
                    bw.write("</aiml>\n");
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }

        }
        File dir = new File(MagicStrings.aiml_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * load bot properties
     */
    void addProperties() {
        try {
            properties.getProperties(MagicStrings.config_path+"/properties.txt");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

    static int leafPatternCnt = 0;
    static int starPatternCnt = 0;

    /** find suggested patterns in a graph of inputs
     *
     */
    public void findPatterns() {
        findPatterns(inputGraph.root, "");
        System.out.println(leafPatternCnt+ " Leaf Patterns "+starPatternCnt+" Star Patterns");
    }

    /** find patterns recursively
     *
     * @param node                      current graph node
     * @param partialPatternThatTopic   partial pattern path
     */
    void findPatterns(Nodemapper node, String partialPatternThatTopic) {
        if (NodemapperOperator.isLeaf(node)) {
            //System.out.println("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic);
            if (node.category.getActivationCnt() > MagicNumbers.node_activation_cnt) {
                //System.out.println("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic+" "+node.shortCut);    //Start writing to the output stream
                leafPatternCnt ++;
                try {
                    String categoryPatternThatTopic = "";
                    if (node.shortCut) {
                        //System.out.println("Partial patternThatTopic = "+partialPatternThatTopic);
                        categoryPatternThatTopic = partialPatternThatTopic + " <THAT> * <TOPIC> *";
                    }
                    else categoryPatternThatTopic = partialPatternThatTopic;
                    Category c = new Category(0, categoryPatternThatTopic,  MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                    //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
                    //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
                    if (!brain.existsCategory(c) && !deletedGraph.existsCategory(c) && !unfinishedGraph.existsCategory(c)) {
                        patternGraph.addCategory(c);
                        suggestedCategories.add(c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(NodemapperOperator.size(node) > MagicNumbers.node_size) {
            //System.out.println("STAR: "+NodemapperOperator.size(node)+". "+partialPatternThatTopic+" * <that> * <topic> *");
            starPatternCnt ++;
            try {
                Category c = new Category(0, partialPatternThatTopic+" * <THAT> * <TOPIC> *",  MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
                //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
                if (!brain.existsCategory(c) && !deletedGraph.existsCategory(c) && !unfinishedGraph.existsCategory(c)) {
                    patternGraph.addCategory(c);
                    suggestedCategories.add(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String key : NodemapperOperator.keySet(node)) {
            Nodemapper value = NodemapperOperator.get(node, key);
            findPatterns(value, partialPatternThatTopic + " " + key);
        }

    }

    /** classify inputs into matching categories
     *
     * @param filename    file containing sample normalized inputs
     */
    public void classifyInputs (String filename) {
        try{
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            while ((strLine = br.readLine())!= null)   {
                // Print the content on the console
                //System.out.println("Classifying "+strLine);
                if (strLine.startsWith("Human: ")) strLine = strLine.substring("Human: ".length(), strLine.length());
                Nodemapper match = patternGraph.match(strLine, "unknown", "unknown");
                match.category.incrementActivationCnt();
                count += 1;
            }
            //Close the input stream
            br.close();
        } catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    /** read sample inputs from filename, turn them into Paths, and
     * add them to the graph.
     *
     * @param filename file containing sample inputs
     */
    public void graphInputs (String filename) {
        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                //strLine = preProcessor.normalize(strLine);
                Category c = new Category(0, strLine, "*", "*", "nothing", MagicStrings.unknown_aiml_file);
                Nodemapper node = inputGraph.findNode(c);
                if (node == null) {
                  inputGraph.addCategory(c);
                  c.incrementActivationCnt();
                }
                else node.category.incrementActivationCnt();
                //System.out.println("Root branches="+g.root.size());
            }
            //Close the input stream
            br.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }



    /**
     * read AIMLIF categories from a file into bot brain
     *
     * @param filename    name of AIMLIF file
     * @return   array list of categories read
     */
    public ArrayList<Category> readIFCategories (String filename) {
        ArrayList<Category> categories = new ArrayList<Category>();
        try{
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                try {
                    Category c = Category.IFToCategory(strLine);
                    categories.add(c);
                } catch (Exception ex) {
                    System.out.println("Invalid AIMLIF in "+filename+" line "+strLine);
                }
            }
            //Close the input stream
            br.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return categories;
    }

    /**
     * check Graphmaster for shadowed categories
     */
    public void shadowChecker () {
        shadowChecker(brain.root) ;
    }

    /** traverse graph and test all categories found in leaf nodes for shadows
     *
     * @param node
     */
    void shadowChecker(Nodemapper node) {
        if (NodemapperOperator.isLeaf(node)) {
            String input = node.category.getPattern().replace("*", "XXX").replace("_", "XXX");
            String that = node.category.getThat().replace("*", "XXX").replace("_", "XXX");
            String topic = node.category.getTopic().replace("*", "XXX").replace("_", "XXX");
            Nodemapper match = brain.match(input, that, topic);
            if (match != node) {
                System.out.println("" + Graphmaster.inputThatTopic(input, that, topic));
                System.out.println("MATCHED:     "+match.category.inputThatTopic());
                System.out.println("SHOULD MATCH:"+node.category.inputThatTopic());
            }
        }
        else {
            for (String key : NodemapperOperator.keySet(node)) {
                shadowChecker(NodemapperOperator.get(node, key));
            }
        }
    }

    /**
     * Load all AIML Sets
     */
    void addAIMLSets() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.sets_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                System.out.println("Loading AIML Sets files from "+MagicStrings.sets_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            System.out.println(file);
                            String setName = file.substring(0, file.length()-".txt".length());
                            System.out.println("Read AIML Set "+setName);
                            AIMLSet aimlSet = new AIMLSet(setName);
                            aimlSet.readAIMLSet(this);
                            setMap.put(setName, aimlSet);
                        }
                    }
                }
            }
            else System.out.println("addAIMLSets: "+MagicStrings.sets_path+" does not exist.");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

    /**
     * Load all AIML Maps
     */
    void addAIMLMaps() {
        Timer timer = new Timer();
        timer.start();
        try {
            // Directory path here
            String file;
            File folder = new File(MagicStrings.maps_path);
            if (folder.exists()) {
                File[] listOfFiles = folder.listFiles();
                System.out.println("Loading AIML Map files from "+MagicStrings.maps_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            System.out.println(file);
                            String mapName = file.substring(0, file.length()-".txt".length());
                            System.out.println("Read AIML Map "+mapName);
                            AIMLMap aimlMap = new AIMLMap(mapName);
                            aimlMap.readAIMLMap(this);
                            mapMap.put(mapName, aimlMap);
                        }
                    }
                }
            }
            else System.out.println("addCategories: "+MagicStrings.aiml_path+" does not exist.");
        } catch (Exception ex)  {
            ex.printStackTrace();
        }
    }

}
