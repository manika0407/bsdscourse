package com.project.bsds.bsdsassignment.db;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UserInfoDB {
  private static final int MAX_USERS = 5000;
  private static String fileName = "user_db/user_info.csv";

  private List<UserInfo> userInfoList;

  public UserInfoDB() {
    userInfoList = new ArrayList<>();
  }

  public UserInfo getUserInfo(int userId) {
    if (userId > userInfoList.size() || userId < 1) {
      throw new IllegalArgumentException("out of range userId: " + userId);
    }
    UserInfo userInfo = userInfoList.get(userId - 1);
    if (userInfo.getUserId() != userId) {
      throw new IllegalArgumentException("invalid object for userId: " + userId);
    }
    return userInfo;
  }

  public void loadData() throws IOException {
    URL fileUrl = UserInfoDB.class.getClassLoader().getResource(fileName);
    if (fileUrl == null) {
      throw new IllegalArgumentException("path doesn't exist");
    }
    File file = new File(fileUrl.getPath());
    FileReader filereader = new FileReader(file);
    CSVReader csvReader = new CSVReader(filereader);
    String[] nextRecord;
    boolean readHeader = false;

    userInfoList.clear();
    while ((nextRecord = csvReader.readNext()) != null) {
      if (nextRecord.length != 3) {
        throw new IllegalArgumentException("incorrectly formatted csv.");
      }
      if (!readHeader) {
//        System.out.print("UserDB Header -> ");
        for (String cell : nextRecord) {
//          System.out.print(cell + "\t");
        }
//        System.out.println();
        readHeader = true;
        continue;
      }
      UserInfo temp =
          new UserInfo(
              Integer.parseInt(nextRecord[0]),
              State.valueOfAbbreviation(nextRecord[1]),
              Gender.valueOf(nextRecord[2]));
      userInfoList.add(temp);
    }
    csvReader.close();
  }

  private static void generateData() throws IOException {
    URL folderUrl = UserInfoDB.class.getClassLoader().getResource("com/project/bsds/bsdsassignment/db");
    if (folderUrl == null) {
      throw new IllegalArgumentException("path doesn't exist");
    }
    File file = new File(folderUrl.getPath(), fileName);
    FileWriter outputfile = new FileWriter(file);
    CSVWriter writer = new CSVWriter(outputfile);
    String[] header = {"user_id", "state", "gender"};
    writer.writeNext(header);
    String[] data = new String[3];
    int numStates = State.LAST.ordinal();
    ThreadLocalRandom tr = ThreadLocalRandom.current();
    for (int i = 1; i <= MAX_USERS; i++) {
      data[0] = String.valueOf(i);
      data[1] = State.values()[tr.nextInt(numStates)].getAbbreviation();
      data[2] = Gender.values()[tr.nextInt(2)].toString();
      writer.writeNext(data);
    }
    writer.close();
  }

  public static void main(String[] args) throws IOException {
    UserInfoDB userInfoDB = new UserInfoDB();
    userInfoDB.loadData();
    System.out.println(userInfoDB.getUserInfo(1));
  }
}
