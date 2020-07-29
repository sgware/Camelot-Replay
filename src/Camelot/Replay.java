package Camelot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

public class Replay {
	List<String> console;
	String path;
	List<String> messages;
	Entry arrivedMsg;
	String player;
	List<String> succeedWaitList;
	public Replay(String path) {
		this.path=path;
		console=new ArrayList<String>();
		messages=new ArrayList<String>();
		succeedWaitList=new ArrayList<String>();
	}

	public void Add(String msg) {
		if(!msg.startsWith("started")&&
				(msg == "input Reset()"|| msg == "input Close Narration" || !msg.startsWith("input"))) {
			synchronized(console) {
				console.add(msg);
			}
		}

	}

	public class Entry{
		public String action;
		public int stamp;

		public Entry(String line) {
			String[] values = line.split("\t");
			action=values[1];
			Matcher m = Pattern.compile("\\d+:(\\d+):(\\d+)").matcher(values[0]);
			if (m.find()) {
				stamp= Integer.parseInt(m.group(1))*60+Integer.parseInt(m.group(2));
			}
		}

		long Difference(Entry other) {
			return 1000*(stamp-other.stamp);
		}
	}

	String GetCoordinates(String log){
		Matcher m = Pattern.compile("\\(([^\\)]+)\\)").matcher(log);
		if (m.find( )) {
			return m.group(1);
		}
		else {
			return null;
		}
	}

	private String Start(String log)
	{
		return command("start", log);
	}

	private String Fail(String log)
	{
		return command("stop", log);
	}

	private String Succeeded(String log)
	{
		return command("succeeded",log);
	}

	private String Failed(String log)
	{
		return command("failed", log);
	}

	private String command(String type, String log)
	{
		return type + log.substring(log.indexOf(' '), log.length());
	}

	private boolean IgnoreList(String log)
	{
		String[] ignoreList = new String[]{
				"EnableIcon", "DisableIcon", "EnableInput", "DisableInput"
		};
		for(String item:ignoreList) {
			if(item==log)
				return true;
		}
		return false;
	}

	private void WalkToSpot(Entry log)
	{
		String coordinates = GetCoordinates(log.action);
		if (coordinates!=null)
		{
			if (log.action.startsWith("input arrived"))
				arrivedMsg = log;
			String walktospot = " WalkToSpot("+player+", "+ coordinates+")";
			System.out.println(Start(walktospot));
			String stopping = Succeeded(walktospot);
			messages.add("@Waiting for: " + stopping);
			while (!console.contains(Succeeded(walktospot)))
			{
				if (console.contains(Succeeded(walktospot)))
					break;
				boolean fail=false;
				for (int j = 0; j < console.size(); j++)
				{
					String msg = console.get(j);
					if (msg != null && msg.startsWith(Failed(walktospot)))
					{
						fail = true;
						break;
					}
				}
				if (fail)
					break;
			}
			messages.add("@Received for: " + stopping);
			synchronized(console)
			{
				console.remove(stopping);
			}
		}
	}

	public void Start() {
		try {
			List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
			List<Entry> logs = new ArrayList<Entry>();
			for(String line:lines) {
				if(line.contains("\t"))
					logs.add(new Entry(line));
			}

			System.out.println("start DisableInput()");
			for(int i=0;i<logs.size();i++) {
				Entry log=logs.get(i);
				if(i>0) {
					try {
						long wait = log.Difference(logs.get(i-1));
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(IgnoreList(log.action)) {
					continue;
				}
				if (log.action.startsWith("started"))
				{
					if (player != null && log.action.contains("WalkTo(" + player))
						continue;

					while (succeedWaitList.size()> 0)
					{
						ArrayList<String> intersection = new ArrayList<String>();
						for (int j = 0; j < succeedWaitList.size(); j++)
						{
							if (console.contains(succeedWaitList.get(j)))
								intersection.add(succeedWaitList.get(j));
						}
						synchronized(console)
						{
							for (int j = 0; j < intersection.size(); j++)
							{
								console.remove(intersection.get(j));
								succeedWaitList.remove(intersection.get(j));
							}
						}
					}

					String msg = Start(log.action);
					System.out.println(msg);
					synchronized(console)
					{
						console.remove(log.action);
					}

					String camerafocus = "started SetCameraFocus(";
					if (log.action.startsWith(camerafocus))
						player = log.action.substring(log.action.indexOf(camerafocus) + camerafocus.length(), log.action.indexOf(")"));


				}
				else if (log.action.startsWith("succeeded"))
				{
					if (player != null && log.action.contains("WalkTo(" + player))
						continue;

					succeedWaitList.add(log.action);
				}
				else if (log.action.startsWith("failed"))
				{
					String msg = Fail(log.action);
					System.out.println(msg);
				}
				else if (log.action.startsWith("input started"))
				{
					int j = 0;
					for (j = i + 1; !logs.get(j).action.startsWith("input arrived") &&
							!logs.get(j).action.startsWith("input stopped") &&
							//!logs[j].action.StartsWith("input started") &&
							j < logs.size(); j++)
						continue;
					if (player != null)
						WalkToSpot(logs.get(j));

				}
				else if (log.action.startsWith("input arrived"))
				{
					if (log != arrivedMsg)
					{
						if (player != null)
							WalkToSpot(log);
					}
				}
			}
			System.out.println("start SetNarration('Log Replay Completed')");
			System.out.println("start ShowNarration()");
			while (!console.contains("input Close Narration") && !console.contains("input Quit"))
				continue;
			if (console.contains("input Close Narration"))
			{
				System.out.println("start Quit()");
				while (!console.contains("succeeded Quit()"))
					continue;
			}


		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}finally {

		}
	}


}
