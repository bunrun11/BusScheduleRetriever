package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetrieverApplication {

	public static LinkedHashMap<String, LinkedHashMap<String, String>> schedule;
	public static void main(String[] args) {
		try(Scanner scanner = new Scanner(System.in)){
			schedule = SplitSchedulePage();			
			
			//Asks the user for a few letters of the destination they want
			ArrayList<String> cities = new ArrayList<String>();
			String userInput;
			do {
				System.out.print("Please enter the first few letters that your destination start with: ");
				userInput = scanner.nextLine();
			} while((cities = getValidDestinations(userInput)).size() == 0);
			
			//Asks user for the bus they want
			String busLink;
			do {
				System.out.print("Please enter a route ID: ");
				userInput = scanner.next();
			} while((busLink = getBusLink(userInput, cities)) == null);
			
			System.out.println();
			System.out.printf("The link for your route is: https://www.communitytransit.org/busservice%s\n", busLink);
			System.out.println();
			
			//Gets and parses the bus route
			String pageText = getPageText("https://www.communitytransit.org/busservice" + busLink);
			Pattern busRoute = Pattern.compile("(?s)(?<=<h2>Weekday<small>)(.*?)</small>.*?(?=</thead>)");
			Matcher busRouteMatcher = busRoute.matcher(pageText);
			Pattern busStop = Pattern.compile("(?s)<strong.*?>(.*?)(?=</strong>).*?(?<=<p>)(.*?)(?=</p>)");
			while(busRouteMatcher.find()) {
				String busDestination = busRouteMatcher.group(1);
				System.out.printf("Destination: %s\n", busDestination);
				String busRoutes = busRouteMatcher.group(0);				
				Matcher busStopMatcher = busStop.matcher(busRoutes);
				while(busStopMatcher.find()) {
					System.out.printf("Stop: %s, %s\n", busStopMatcher.group(1), busStopMatcher.group(2));
				}
				System.out.println("+++++++++++++++++++++++++++++++++++");
			}
			
		} catch (MalformedURLException e) {
			System.out.println("ERROR!");
		} catch (IOException e) {
			System.out.println("ERROR!");
		}
	}
	
	//Gets the link extension for the requested bus
	public static String getBusLink(String busNumber, ArrayList<String> cities) {
		for(String city : cities){
			if(schedule.get(city).get(busNumber) != null) {
				return schedule.get(city).get(busNumber);
			}
		}
		return null;
	}
	
	//Gets and parses the main schedules' webpage
	public static LinkedHashMap<String, LinkedHashMap<String, String>> SplitSchedulePage() throws MalformedURLException, IOException{
		String schedulerURLString = "https://www.communitytransit.org/busservice/schedules/";
		String pageText = getPageText(schedulerURLString);
		LinkedHashMap<String, LinkedHashMap<String, String>> schedule = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		
		Pattern cityPattern = Pattern.compile("(?s)(?<=<h3>)(\\w.*?)(?=</h3>)(.*?(?=<h3>|<p>))");
		Matcher cityMatcher = cityPattern.matcher(pageText);
		
		Pattern busPattern = Pattern.compile("(?<=<strong><a href=\")(.*?)\".*?>(.*?)(?: |</a>)");
		while(cityMatcher.find()) {
			String city = cityMatcher.group(1);
			String cityBuses = cityMatcher.group(2);
			schedule.putIfAbsent(city, new LinkedHashMap<String, String>());
			Matcher busMatcher = busPattern.matcher(cityBuses);
			while(busMatcher.find()) {
				String busURL = busMatcher.group(1);
				String busNumber = busMatcher.group(2);				
				schedule.get(city).put(busNumber, busURL);
			}
		}
		return schedule;
	}
	
	//returns the destinations that start with some string
	public static ArrayList<String> getValidDestinations(String startsWith) {
		ArrayList<String> cities = new ArrayList<String>();
		for(String city : schedule.keySet()) {
			if(city.startsWith(startsWith)) {
				cities.add(city);
				System.out.printf("Destination: %s\n", city);
				for(String busNumber : schedule.get(city).keySet()) {
					System.out.printf("Bus Number: %s\n", busNumber);
				}
				System.out.println("+++++++++++++++++++++++++++++++++++");
			}
		}
		return cities;
	}
	
	//Gets the text from a website
	public static String getPageText(String URLString) throws MalformedURLException, IOException {
		URLConnection URL = new URL(URLString).openConnection();
		//URL.setRequestProperty("user-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		try(BufferedReader in = new BufferedReader(new InputStreamReader(URL.getInputStream()))){
			String inputLine = "";
			StringBuilder text = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				text.append(inputLine.replaceAll("&amp;", "&") + "\n");
			}
			return text.toString();
		}
	}
	
	//prints the schedule out
	public static void printBusSchedule() {
		for(Map.Entry<String, LinkedHashMap<String, String>> citySchedule : schedule.entrySet()) {
			System.out.println(citySchedule.getKey());
			for(Map.Entry<String, String> busSchedule : citySchedule.getValue().entrySet()) {
				System.out.printf("\t%s\t%s\n", busSchedule.getKey(),busSchedule.getValue());
			}
		}
	}
}
