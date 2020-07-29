package Camelot;

import java.io.File;
import java.util.Scanner;
import java.lang.String;

public class CamelotReplay {
	public static Replay replay;
	static void Listen() {
	}
	
	public static void main(final String[] args) {
		if(args.length>0) {
			File f = new File(args[0]);
			if(f.exists() && !f.isDirectory()) { 
			    replay = new Replay(args[0]);
			    Listener thread=new Listener();
			    thread.start();
			    replay.Start();
			}else {
				Message("The specified file does not exist");
			}
		}else {
			Message("You must specify a log file as the first argument");
		}
	}
	
	static void Message(String message) {
		System.out.println("start ShowMenu()");
		System.out.println("start HideMenu()");
		System.out.println("start SetNarration("+message+")");
		System.out.println("start ShowNarration()");
		Scanner s = new Scanner(System.in);
		while(true) {
			String msg = s.nextLine();
			if(msg != null && msg == "input Close Narration()") {
				System.out.println("start Quit()");
				break;
			}
		}
		s.close();
	}
}
