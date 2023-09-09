package org.example;

import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvValidationException;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class Main {
    static int capacity = 5;
    static ArrayBlockingQueue<String> messageQueue = new ArrayBlockingQueue<String>(capacity);

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            try {
                serverStart();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

//        serverStart();

        new Thread(() -> {
            try {
                while (true) {
                    String message = messageQueue.take();
                    printLog(message);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    public static void printLog(String log) {
        try (FileWriter writer = new FileWriter("log.csv", true)) {
            writer.write(log + "\r\n");
            writer.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void serverStart() throws InterruptedException {
        String[] settingsColumnMapping = {"server", "port"};
        String settingsFileName = "Settings.txt";
        List<Settings> settings = parseSettingsCSV(settingsColumnMapping, settingsFileName);
        int port;
        port = settings.get(0).port;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                try (
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ) {
                    while (!in.ready()) ;

                    while (in.ready()) {

                        System.out.println(in.readLine());
                        String result = in.readLine();
                        int a = result.indexOf(",");
                        String path = result.substring(0, a);
                        String message = result.substring(a + 1, result.length());

                        if (!message.substring(message.length() - 8).equals("/refresh")) {
//                          printLog(message);
                            messageQueue.put(message);
                        }

                        Thread.sleep(1500);

                        try (BufferedReader br = new BufferedReader(new FileReader("log.csv"))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                out.println(line);
                            }
                        }

                    }

                    System.out.println("Client disconnected!");

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static List<Settings> parseSettingsCSV(String[] columnMapping, String fileName) {

        List<Settings> messages = null;

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            ColumnPositionMappingStrategy<Settings> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(Settings.class);
            strategy.setColumnMapping(columnMapping);

            CsvToBean<Settings> csv = new CsvToBeanBuilder<Settings>(reader)
                    .withMappingStrategy(strategy)
                    .build();

            messages = csv.parse();

            // Массив считанных строк
            String[] nextLine;
            // Читаем CSV построчно
            while ((nextLine = reader.readNext()) != null) {
                // Работаем с прочитанными данными.
                System.out.println(Arrays.toString(nextLine));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return messages;
    }

}