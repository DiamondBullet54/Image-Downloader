import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {

    /* Java twitter image downloader application:
     * 1. Lets the user either paste an image source link or use a button to get the most recent clipboard link.
     * 2. Lets the user select a dropdown category for the image based on a folder and all image names in the folder.
     * 3. A button will download the image and display text saying it was downloaded.
     * 4. (No Plan for Future Implimentation) The user can press the settings page to be able to change the folder location and the dropdown categories manually.
     */

    public static Map<String, Integer> categories = new HashMap<String, Integer>(); //all folder categories
    public static MainGUI frame = new MainGUI(); //the GUI system we use

    public static void main(String[] args) {
        //set up the gui and make it visible (we also make it minimize on close)
        frame.setFocusable(true);
        frame.setVisible(true);
        frame.setTitle("Twitter Image Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        
        /*frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setExtendedState(JFrame.ICONIFIED);
            }
        });*/

        //create settings file if it doesn't exist
        if(!Files.exists(Paths.get("persistantData.txt"))){
            try {
                Files.createFile(Paths.get("persistantData.txt"));
            } catch (Exception e) {
                System.out.println("Error creating settings file: " + e.toString());
            }
        }

        //read the path from the settings file if its not empty
        try {
            String path = Files.readAllLines(Paths.get("persistantData.txt")).get(0);
            frame.folder.setText(path);

            //if it makes it this far, it means it read the settings page, so we can get the categories
            getCategories();
        } catch (Exception e) {
            System.out.println("Error reading settings file: File just created or not found");
        }
    }

    //goes to the folder and returns a map of all image categories and their count
    //if there is no folder on the path, it will return an empty map
    public static void getCategories(){
        try {
            //get the folder path from the GUI
            File folder = new File(frame.folder.getText());

            //get all files in the folder
            File[] listOfFiles = folder.listFiles();

            //remove all current category countss
            categories.clear();
            
            //now that we got the file list, we need to break up the file name so we can get the category
            for (File file : listOfFiles) {
                //get the file name
                String fileName = file.getName();
                
                //get category which is everything before the first number or period
                String category = "";
                for(int i = 0; i < fileName.length(); i++){
                    if(Character.isDigit(fileName.charAt(i)) || fileName.charAt(i) == '.'){
                        break;
                    }
                    else{
                        category += fileName.charAt(i);
                    }
                }

                //if the file is not in the map, add it
                if(!categories.containsKey(category)){
                    categories.put(category, 1);
                }
                //if the file is in the map, increment it
                else{
                    categories.put(category, categories.get(category) + 1);
                }
            }
            System.out.println(categories.toString());

        } catch (Exception e) {
            frame.output.setText("Categories not found");
            System.out.println("Error getting categories: " + e.toString());
        }

        //sort all categories by int value
        categories = categories.entrySet()
        .stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, java.util.LinkedHashMap::new));

        //remove all categories from the list
        frame.category.removeAllItems();

        //for each category, add it to the dropdown
        for(String category : categories.keySet()){
            frame.category.addItem(category);
        }

        //display "Categories Loaded" on the GUI if category count is greater than 0
        if(categories.size() > 0){
            frame.output.setText("Categories Loaded!");
        }

        //save the folder path to the persistantData file
        try {
            Files.write(Paths.get("persistantData.txt"), frame.folder.getText().getBytes());
        } catch (Exception e) {
            System.out.println("Error writing to persistantData file: " + e.toString());
        }
    }

    //returns a true/false if the image was downloaded or not based on the link
    public static void downloadImage(){
        try {
            System.out.println("Downloading image!");

            //to get the download page for a twitter(x) image, you need to change the name to large
            String link = frame.link.getText();
            //replace "small" or "medium" with "large"
            link = link.replace("small", "large");
            link = link.replace("medium", "large");
            URL url = new URL(link);

            //get the format type from the URL
            String format = getFormatName(url);

            //get the image from the URL
            RenderedImage image = ImageIO.read(url);

            String name = frame.category.getSelectedItem().toString(); //get the name from the GUIs category text box
            int count = categories.get(name); //get the number of images in the category
            name += count + 1; //add 1 to the count and make it the output name

            //download the image to the folder
            Path savePath = Paths.get(frame.folder.getText() + "\\" + name + "." + format); //folder path + name + format
            File file = savePath.toFile();

            //if file does not exist, create it
            if(!file.exists()){
                ImageIO.write(image, format, file);
            }else{
                //if the file exits, then the code is broken or the user has a missing image.
                //don't save the file, and warn the user to fix the file formatting issue.
                frame.output.setText("FILE " + name + " EXISTS! EXITING!");
                return;
            }
            
            System.out.println("Downloading image " + name + "." + format + " to " + frame.folder.getText());
            frame.output.setText("Downloaded Image " + name);

            //get the category from the GUI box
            String category = frame.category.getSelectedItem().toString();
            //add 1 to the category count
            categories.put(category, categories.get(category) + 1);
            
        } catch (Exception e) {
            frame.output.setText("Could Not Download File");
            System.out.println(e.toString());
        }
    }

    //returns the format type (jpeg, jpg, png, etc) from the URL
    public static String getFormatName(URL url) {
        String query = url.getQuery();
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("format=")) {
                return param.substring(7);
            }
        }
        return "jpg"; // default format
    }
}