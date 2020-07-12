package com.serverjars.updater;

import com.serverjars.api.JarDetails;
import com.serverjars.api.Response;
import com.serverjars.api.request.AllRequest;
import com.serverjars.api.request.JarRequest;
import com.serverjars.api.request.LatestRequest;
import com.serverjars.api.request.TypesRequest;
import com.serverjars.api.response.AllResponse;
import com.serverjars.api.response.LatestResponse;
import com.serverjars.api.response.TypesResponse;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;

/**
 * @author Jacobtread
 * -
 * created on 7/11/20 at 5:06 PM
 */
public class Updater {


    private static final String BREAK = "#==============================================================#";
    private static final int BREAK_LENGTH = BREAK.length();

    private static String type;
    private static String version;

    public static void main(String[] args) {
        if (getJavaVersion() > 8) {
            err(center("SORRY ServerJars Updater will not run on java version") + "\n" + center("higher than java 8"));
            return;
        }
        System.out.println(BREAK + "\n       _____                               __               \n" +
                "      / ___/___  ______   _____  _____    / /___ ___________\n" +
                "      \\__ \\/ _ \\/ ___/ | / / _ \\/ ___/_  / / __ `/ ___/ ___/\n" +
                "     ___/ /  __/ /   | |/ /  __/ /  / /_/ / /_/ / /  (__  ) \n" +
                "    /____/\\___/_/    |___/\\___/_/   \\____/\\__,_/_/  /____/  \n" +
                center("ServerJars.com      Made with love by Songoda <3") + "\n\n" +
                center("* New Updater By Jacobtread *") + "\n" + BREAK);
        File propertyFile = new File("serverjars.properties");
        if (!propertyFile.exists()) {
            System.out.println(
                    center("You dont have config setup yet, create one? [Y/N]") + "\n" +
                            center("if you choose 'n' a default config will be created instead") +
                            "\n" + BREAK
            );
            String choice = awaitInput(s -> s.equalsIgnoreCase("y") || s.equalsIgnoreCase("n"), "Please choose Y or N");
            if (choice != null) {
                if (choice.equalsIgnoreCase("y")) {
                    setup();
                } else {
                    Map<String, String> defaultProperties = new HashMap<>();
                    defaultProperties.put("type", "spigot");
                    defaultProperties.put("version", "latest");
                    type = "spigot";
                    version = "latest";
                    Properties.save(defaultProperties, propertyFile);
                }
            }
        } else {
            System.out.println(center("Loading properties file"));
            loadProperties(propertyFile);
        }
        checkUpdates(args);
    }

    private static void checkUpdates(String[] args) {
        System.out.println(center("Checking if jar is up to date"));
        JarDetails jarDetails = null;
        if (version.equals("latest")) {
            LatestResponse latestResponse = new LatestRequest(type).send();
            jarDetails = latestResponse.latestJar;
        } else {
            AllResponse allResponse = new AllRequest(type).send();
            for (JarDetails jar : allResponse.getJars()) {
                if (jar.getVersion().equalsIgnoreCase(version)) {
                    jarDetails = jar;
                }
            }
            if (jarDetails == null) {
                err(center(String.format("Couldn't find ServerJar for '%s' of '%s'", version, type)));
                return;
            }
        }
        boolean isStart;
        File jarFile = new File(type + ".jar");
        if (jarFile.exists()) {
            String hash = md5File(jarFile);
            if (hash.isEmpty() || !hash.equals(jarDetails.getHash())) {
                log(center("Current jar seems to be out of date downloading an updated version."));
                isStart = downloadJar(jarFile);
            } else {
                System.out.println(center("Current jar is up to date."));
                isStart = true;
            }
        } else {
            log(center(String.format("Jar missing! downloading '%s' of '%s'", version, type)));
            isStart = downloadJar(jarFile);
        }
        if (isStart) {
            start(jarFile, args);
        }
    }

    private static void start(File jarFile, String[] args) {
        System.out.println(BREAK + "\n" + center(String.format("Starting '%s' version '%s'", type, version)) + "\n" + BREAK);
        final String main = getMainClass(jarFile);
        Method mainMethod = getMainMethod(jarFile, main);

        try {
            mainMethod.invoke(null, new Object[]{args});
        } catch (final IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error while running patched jar");
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static String getMainClass(final File jar) {
        try {
            final InputStream is = new BufferedInputStream(new FileInputStream(jar));
            final JarInputStream js = new JarInputStream(is);
            return js.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (final IOException e) {
            System.err.println("Error reading from patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

    private static Method getMainMethod(final File jar, final String mainClass) {
        Agent.addToClassPath(jar);
        try {
            final Class<?> cls = Class.forName(mainClass, true, ClassLoader.getSystemClassLoader());
            return cls.getMethod("main", String[].class);
        } catch (final NoSuchMethodException | ClassNotFoundException e) {
            System.err.println("Failed to find main method in patched jar");
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

    private static boolean downloadJar(File jarFile) {
        Response response = new JarRequest(type, version.equalsIgnoreCase("latest") ? null : version, jarFile).send();
        if (response.isSuccess()) {
            log(center("Jar downloaded successfully"));
            return true;
        } else {
            err(center("Failed to download jar: " + response.getErrorMessage()));
            return false;
        }
    }

    private static String md5File(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(readBytes(file));
            BigInteger bigInt = new BigInteger(1, digest);
            StringBuilder hash = new StringBuilder(bigInt.toString(16));
            while (hash.length() < 32) {
                hash.insert(0, "0");
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    private static byte[] readFully(final InputStream in, final int size) throws IOException {
        try {
            final int bufSize;
            if (size == -1) {
                bufSize = 16 * 1024;
            } else {
                bufSize = size;
            }
            byte[] buffer = new byte[bufSize];
            int off = 0;
            int read;
            while ((read = in.read(buffer, off, buffer.length - off)) != -1) {
                off += read;
                if (off == buffer.length) {
                    buffer = Arrays.copyOf(buffer, buffer.length * 2);
                }
            }
            return Arrays.copyOfRange(buffer, 0, off);
        } finally {
            in.close();
        }
    }

    private static byte[] readBytes(final File file) {
        try {
            return readFully(new FileInputStream(file), (int) file.length());
        } catch (final IOException e) {
            err(center("Failed to read all of the data from " + file.getAbsolutePath()));
            e.printStackTrace();
            System.exit(1);
            throw new InternalError();
        }
    }

    private static void loadProperties(File propertyFile) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(propertyFile));
            StringBuilder contents = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                contents.append(line).append("\n");
            }
            Map<String, String> properties = Properties.parse(contents.toString());
            if (properties.containsKey("type")) {
                type = properties.get("type");
            } else {
                type = "spigot";
                err(center("Properties file missing type defaulting to spigot"));
            }
            if (properties.containsKey("version")) {
                version = properties.get("version");
            } else {
                version = "latest";
                err(center("Properties file missing version defaulting to latest"));
            }
            System.out.println(center("Loaded properties file") + "\n" + BREAK);
        } catch (IOException e) {
            err("Unable to load serverjars.properties: ");
            e.printStackTrace();
        }
    }

    private static void setup() {
        log(center("Connecting to ServerJars.com to find available jar types"));
        TypesResponse typesResponse = new TypesRequest().send();
        if (typesResponse.isSuccess()) {
            Map<String, List<String>> typeMap = typesResponse.getAllTypes();
            List<String> types = new ArrayList<>();
            for (List<String> typeList : typeMap.values()) {
                types.addAll(typeList);
            }
            if (types.size() > 0) {
                log(center("What kind of server would you like to run?") + "\n" + center("* Available server types: *"));
                StringBuilder typeString = new StringBuilder();
                int i = 0;
                for (String type : types) {
                    if (i == 6) {
                        typeString.append("\n");
                        i = 0;
                    }
                    typeString.append(type).append(", ");
                    i++;
                }
                typeString.append("\n");
                System.out.println(typeString + BREAK);
                String chosenJar = awaitInput(s -> types.contains(s.toLowerCase()), "The jar type '%s' was not listed above in the type list\nplease choose another");
                if (chosenJar == null) {
                    chosenJar = "spigot";
                    err(center("Unable to get user input -> defaulting to spigot"));
                }
                type = chosenJar;
                log(center("What version of your server would you like to run?") + "\n" + center("leave blank or type 'latest' for latest"));
                String chosenVersion = awaitInput(s -> true, "Hmm.. that version was somehow incorrect");
                if (chosenVersion != null && chosenVersion.isEmpty()) {
                    chosenVersion = "latest";
                }
                if (chosenVersion == null) {
                    chosenVersion = "latest";
                    err(center("Unable to get user input -> defaulting to latest"));
                }
                version = chosenVersion;
                log(center("Setup completed! Saving to serverjars.properties"));
                Map<String, String> properties = new HashMap<>();
                properties.put("type", type);
                properties.put("version", version);
                Properties.save(properties, new File("serverjars.properties"));
            } else {
                err(center("No types could be found -> defaulting to spigot"));
            }
        } else {
            err(center("Unable to retrieve types from ServerJars.com") + "\n" + BREAK + "\nError Title: " + typesResponse.getErrorTitle() + "\nError Message: " + typesResponse.getErrorMessage());
        }
    }

    static void log(String text) {
        System.out.println("\n\n" + BREAK + "\n" + text + "\n" + BREAK);
    }

    static String center(String text) {
        StringBuilder spaceBuilder = new StringBuilder();
        for (int i = 0; i < BREAK_LENGTH / 2 - text.length() / 2; i++) {
            spaceBuilder.append(" ");
        }
        return spaceBuilder.toString() + text;
    }

    static void err(String text) {
        System.err.println("\n\n" + BREAK + "\n" + text + "\n" + BREAK);
    }

    private static String awaitInput(Predicate<String> predicate, String errorMessage) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (predicate.test(line)) {
                    return line;
                } else {
                    System.err.println(BREAK + "\n" + String.format(errorMessage, line) + "\n" + BREAK);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

}
