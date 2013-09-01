package org.alicebot.ab.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class IOUtils {


	public static String readInputTextLine() {
        BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in));
		String textLine = null;
		try {
			textLine = lineOfText.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return textLine;
	}


	public static String system(String evaluatedContents, String failedString) {
		Runtime rt = Runtime.getRuntime();
        System.out.println("System "+evaluatedContents);
        try {
            Process p = rt.exec(evaluatedContents);
            InputStream istrm = p.getInputStream();
            InputStreamReader istrmrdr = new InputStreamReader(istrm);
            BufferedReader buffrdr = new BufferedReader(istrmrdr);
            String result = "";
            String data = "";
            while ((data = buffrdr.readLine()) != null) {
                result += data+"\n";
            }
            System.out.println("Result = "+result);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return failedString;
        }
	}
}

