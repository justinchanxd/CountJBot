import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
//mongodb
import com.mongodb.*;
import com.mongodb.client.*;

import static com.mongodb.client.model.Filters.*;
//jodatime
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
//telegram api
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class nojBot extends TelegramLongPollingBot {

    final String dateRE = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$";
    final String dayRE = "/start [0-9]{1,6}$";
    final private String apitoken = ""; //Enter bot api token from botFather
    final private long admintgId = 0;
    final private long reallifeId = 0; 
    final private String botVer = "1.7.0";

    MongoClientURI connectionString = new MongoClientURI(""); //database link
    MongoClient mongoClient = new MongoClient(connectionString);
    MongoDatabase database = mongoClient.getDatabase("nojbot");
    MongoCollection<Document> collection = database.getCollection("user");

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            long channel_id = update.getMessage().getChat().getId();
            long user_id = update.getMessage().getFrom().getId();
            int msgId = update.getMessage().getMessageId();
//            next two lines for testing different users, which is my id +1
//            final long uid= update.getMessage().getFrom().getId() ;
//            long user_id = uid+1;
            String userFirstname = update.getMessage().getFrom().getFirstName();
            String userLastname = update.getMessage().getFrom().getLastName();
            String user_fullname = getFullname(userFirstname, userLastname);

            String user_username = update.getMessage().getFrom().getUserName();
            addFullname(user_id, user_username, user_fullname);

            System.out.println("Channel id: " + channel_id + ", User id: " + user_id + ", fullname: " + user_fullname);
            // Set variables
            String msg = update.getMessage().getText();
            //add last access date

            try {

                if (userExist(user_id, channel_id)) {
                    lastAccessDateAdd(user_id);
                }
                //echo from internal usage
                if (user_id == admintgId && msg.startsWith("/echo ")) {
                    makeLog(channel_id, user_id, "/echo");
                    echo(msg);
                }
                //create record
                else if (msg.equals("/start") || msg.equals("/start@CountJBot")) {
                    makeLog(channel_id, user_id, "/start");
                    if (userExist(user_id, channel_id)) {
                        replyUser(channel_id, "系統已經有你嘅記錄!", msgId);
                    } else {
                        replyUser(channel_id, "請填上今日day數 e.g.你今日day 7\n打/start 7", msgId);
                    }
                }
                //create record but wrong format
                else if (msg.startsWith("/start")) {
                    makeLog(channel_id, user_id, "/start [day]");
                    String startDateString = getDatebyDay(msg, channel_id);

                    if (userExist(user_id, channel_id)) {
                        replyUser(channel_id, "系統已經有你嘅記錄!", msgId);
                    } else if (startDateString != "null") {
                        createUser(user_id, user_username, user_fullname, channel_id, startDateString, msgId);
                    } else {
                        replyUser(channel_id, "日期格式唔啱,請入多次", msgId);
//                Get start date by entering date
//                replyUser(channel_id, "請填上開始戒的日子(e.g. 2017-01-16)");
//                String startDateString;
//                String correctDate = checkDate(msg, channel_id);
//                LocalDate startDate = getDatebyDate(correctDate, channel_id);

                        //if it passes the joda date check
//                if (startDate != null) {
//                    startDateString = startDate.toString();
//                    System.out.println("database storing: "+startDateString);
//                    //Not writing current date to database, just for calculating days
//
//                    if (compareDate(startDate, currentDate)) {
//                        replyUser(channel_id, "Day "
//                                + String.valueOf(Days.daysBetween(startDate, currentDate).getDays())+
//                        ", 資料傳送中...");
//                        createUser(user_id, user_username, collection, channel_id, startDateString);
//                    } else {
//                       // replyUser(channel_id, correctDate + "? 而家戒啦仲等!再入過!");
//                    }
//
//                }
                    }
                }
                //help command
                else if (msg.equals("/help") || msg.equals("/help@CountJBot")) {
                    makeLog(channel_id, user_id, "/help");
                    replyUser(channel_id, "CountJBot指令:\n/start [day]: 建立戒J記錄 \n" +
                            "/status: 查看戒J記錄\n/times: 查看破戒記錄\n/jed: 破戒\n注意:一但成功建立記錄,不能再新增記錄\n" +
                            "/update: 更新個人資料\n/feedback [message]: 傳送反饋\n如有任何疑問,請搵@justinchanxd\nBot Version: " + botVer, msgId);
                }
                // Check if user data exist in database before doing actions
                else if ((msg.equals("/status") || msg.equals("/status@CountJBot") || msg.equals("/jed") || msg.equals("/jed@CountJBot") ||
                        msg.equals("我破戒了...") || msg.equals("無J到打錯command :)") ||
                        msg.equals("/update") || msg.equals("/update@CountJBot") || msg.equals("/times") ||
                        msg.equals("/times@CountJBot")) && !userExist(user_id, channel_id)) {
                    replyUser(channel_id, "請先打/start 建立記錄", msgId);
                }
                //check status
                else if (msg.equals("/status") || msg.equals("/status@CountJBot")) {
                    makeLog(channel_id, user_id, "/status");
                    getStatus(user_id, channel_id, msgId);

                }
                //jed
                else if (msg.equals("/jed") || msg.equals("/jed@CountJBot")) {
                    makeLog(channel_id, user_id, "/jed");
                    jedConfirm(user_id, channel_id, msgId);
                }
                //jed confirmed
                else if (msg.equals("我破戒了...")) {
                    makeLog(channel_id, user_id, "/jedTrue");
                    jedCancel(user_id, channel_id, msgId);
                    jed(user_id, channel_id, msgId);
                }
                //jed cancel
                else if (msg.equals("無J到打錯command :)")) {
                    makeLog(channel_id, user_id, "/jedFalse");
                    jedCancel(user_id, channel_id, msgId);
                }
                //update personal information
                else if (msg.equals("/update") || msg.equals("/update@CountJBot")) {
                    makeLog(channel_id, user_id, "/update");
                    updateUser(channel_id, user_id, user_username, user_fullname, msgId);
                }
                //rank function, not built yet
                else if (msg.equals("/rank") || msg.equals("/rank@CountJBot")) {
                }
                //check j times and date
                else if (msg.equals("/times") || msg.equals("/times@CountJBot")) {
                    makeLog(channel_id, user_id, "/times");
                    getjTimesDates(channel_id, user_id, msgId);
                }
                //send feedback to admin, wrong format
                else if (msg.equals("/feedback")) {
                    replyUser(user_id, "請填上訊息內容\nE.g.打/feedback hi", msgId);
                }
                //send feedback to admin
                else if (msg.startsWith("/feedback ") || (msg.startsWith("/feedback ") && msg.endsWith("@CountJBot"))) {
                    msg2Admin(user_id, user_fullname, user_username, msg);
                }
//            else if(msg.equals("/delete")) {
//                deleteDoc(user_id,collection,channel_id,msgId);
//            }
            } catch (Exception e) {
                makeLog(admintgId, user_id, "Error: " + e.getMessage());
            }
        }

    }


    private boolean userExist(long user_id, long channel_id) {
        //check if user data exist in the database
        Document myDoc = collection.find(eq("tg_id", user_id)).first();
        if (myDoc != null) {
            return true;
        } else {
            return false;
        }
    }

    private void createUser(long user_id, String username, String fullname, long channel_id,
                            String startDate, int msgId) {
        //create user record in database
        try {
            Document doc = new Document("tg_id", user_id)
                    .append("username", username)
                    .append("fullname", fullname)
                    .append("startDate", startDate)
                    .append("jTimes", 0);
            collection.insertOne(doc);


            replyUser(channel_id, "記錄建立完成!", msgId);
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
        }

    }

    private void getStatus(long user_id, long channel_id, int msgId) {
        // get user status, day no
        Document myDoc = collection.find(eq("tg_id", user_id)).first();

        String fullname = myDoc.getString("fullname");
        LocalDate startDate = new LocalDate(myDoc.getString("startDate"));
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));
        //Temporary for fixing unrecognized null username user
        if (fullname == null) {
            replyUser(channel_id, "你今日 day" + String.valueOf(Days.daysBetween(startDate, currentDate).getDays()) + "!", msgId);
        } else {
            replyUser(channel_id, fullname + " 今日 day" + String.valueOf(Days.daysBetween(startDate, currentDate).getDays()) + "!",
                    msgId);
        }
    }

    private void jedConfirm(long user_id, long channel_id, int msgId) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(channel_id)
                .setReplyToMessageId(msgId)
                .setText("請按下按鈕確認");
        ReplyKeyboardMarkup keyboardjed = new ReplyKeyboardMarkup();
        keyboardjed.setOneTimeKeyboard(true);
        keyboardjed.setSelective(true);
        // Create the keyboard (list of keyboard rows)
        List<KeyboardRow> keyboard = new ArrayList<>();
        // Create a keyboard row
        KeyboardRow row = new KeyboardRow();
        // Set each button, you can also use KeyboardButton objects if you need something else than text
        row.add("我破戒了...");
        row.add("無J到打錯command :)");
        // Add the first row to the keyboard
        keyboard.add(row);
        keyboardjed.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardjed);
        try {
            execute(message); // Sending our message object to user
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void jedCancel(long user_id, long channel_id, int msgId) {
        //jed action cancel
        SendMessage msg = new SendMessage()
                .setChatId(channel_id)
                .setText("...ok");
        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
        msg.setReplyMarkup(keyboardMarkup);
        try {
            sendMessage(msg); // Call method to send the photo
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void jed(long user_id, long channel_id, int msgId) {
        //confirmed jed action
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));
        Document userDoc = collection.find(eq("tg_id", user_id)).first();
        if (userDoc != null) {
            String username = userDoc.getString("username");
            String fullname = userDoc.getString("fullname");
            int dayNo = getDayNo(user_id);
            Bson updatevalue = new Document("startDate", currentDate.toString());
            Bson updateDoc = new Document("$set", updatevalue);
            collection.updateOne(userDoc, updateDoc);
            //add jTimes
            jTimesAdd(user_id);
            jDatesAdd(user_id);
            //Temporary for fixing unrecognized null username user
            if (fullname == null) {
                replyUser(channel_id, "你已從 day" + dayNo + "降至 day0...", msgId);
            } else {
                replyUser(channel_id, fullname + " 已從 day" + dayNo +
                        " 降至 day0...", msgId);
            }

        }
    }

    private void jTimesAdd(long user_id) {
        //add J times no
        Document userDoc = collection.find(eq("tg_id", user_id)).first();
        //jTimes check
        if (userDoc.getInteger("jTimes") != null) {
            int jTimesInt = userDoc.getInteger("jTimes");
            jTimesInt += 1;
            Bson updatejTimesvalue = new Document("jTimes", jTimesInt);
            Bson updatejTimes = new Document("$set", updatejTimesvalue);
            collection.updateOne(userDoc, updatejTimes);

        } else {
            Bson updatejTimesvalue = new Document("jTimes", 1);
            Bson updatejTimes = new Document("$set", updatejTimesvalue);
            collection.updateOne(userDoc, updatejTimes);
        }

    }

    private void jDatesAdd(long user_id) {
        //add J date
        Document userDoc = collection.find(eq("tg_id", user_id)).first();
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));

        //jDates check
        if (userDoc.getString("jDates") != null) {
            Bson jDates = new Document("jDates", userDoc.getString("jDates") + "," + currentDate.toString());
            Bson updateJDates = new Document("$set", jDates);
            collection.updateOne(userDoc, updateJDates);

        } else {
            Bson jDates = new Document("jDates", currentDate.toString());
            Bson updateJDates = new Document("$set", jDates);
            collection.updateOne(userDoc, updateJDates);
        }
    }

    private void getjTimesDates(long channel_id, long user_id, int msgId) {
        // get jtimes and jdates
        Document myDoc = collection.find(eq("tg_id", user_id)).first();
        if (myDoc != null) {
            String jDates = "";

            int jTimes = 0;
            if (myDoc.getInteger("jTimes") != null) {
                jTimes = myDoc.getInteger("jTimes");
            }
            if (myDoc.getString("jDates") != null) {
                jDates = myDoc.getString("jDates").replaceAll(",", "\n");
            } else {
                System.out.println("No jDates found");
            }


            //Temporary for fixing unrecognized null username user
            if (myDoc.getString("fullname") == null) {
                replyUser(channel_id, "破戒次數: " + jTimes + "\n破戒日期:\n" + jDates, msgId);
            } else {
                replyUser(channel_id, myDoc.getString("fullname") + "\n破戒次數: " + jTimes + "\n破戒日期:\n" + jDates,
                        msgId);
            }
        } else {
            System.out.println("No doc found: ");
        }

    }

    private void replyUser(long channel_id, String msg, int msgId) {
        //send msg to user/group with channel_id
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(channel_id)
                .setText(msg)
                .setReplyToMessageId(msgId);
        try {
            execute(message);
            System.out.println(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void lastAccessDateAdd(long user_id) {
        //add last access date
        Document userDoc = collection.find(eq("tg_id", user_id)).first();
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));

        Bson lad = new Document("lad", currentDate.toString());
        Bson updateLad = new Document("$set", lad);
        collection.updateOne(userDoc, updateLad);

    }

    private void makeLog(long channel_id, long user_id, String cmd) {
        //make log in log file
        DateTime currentTime = new DateTime();
        System.out.println("[" + currentTime + "] ch_id:" + channel_id + " User#" + user_id + "@" + cmd);
    }

    private void msg2Admin(long user_id, String user_fullname, String user_username, String msg) {
        String split[] = msg.split("/feedback ", 2);
        msg = split[1];
        try {
            replyUser(admintgId, "User Feedback:\nUser id: " + user_id + "\nUsername: " +
                    user_username + "\nFullname: " + user_fullname + "\nMessage:\n" + msg, 0);
            replyUser(user_id, "訊息已發送!", 0);
        } catch (Exception e) {
            replyUser(admintgId, "Error message from: " + user_id + "\n" + user_fullname + "\n" + e.toString(), 0);
            replyUser(user_id, "訊息發送失敗!", 0);
        }

    }

    private void updateUser(long channel_id, long user_id, String user_username, String user_fullname, int msgId) {
        //update user profile
        Document myDoc = collection.find(eq("tg_id", user_id)).first();
        if (myDoc != null) {
            //update user info
            Bson updateUsername = new Document("username", user_username);
            Bson updateUDoc = new Document("$set", updateUsername);
            Bson updateFullname = new Document("fullname", user_fullname);
            Bson updateFDoc = new Document("$set", updateFullname);
            collection.updateOne(myDoc, updateUDoc);
            collection.updateOne(myDoc, updateFDoc);
            replyUser(channel_id, "個人資料成功更新!", msgId);
        }
    }

    private void echo(String msg) {
        // speak from bot to admin
        //String[] echoMsg = msg.split("\\s+",2);
        String[] echoMsg = null;
        try {
            echoMsg = msg.split("\\, +", 3);
            replyUser(Long.parseLong(echoMsg[2]), echoMsg[1], 0);
            replyUser(admintgId, "Message sent!", 0);
        } catch (Exception e) {
            replyUser(admintgId, "Wrong echo format, should be: " + '\n' + " /echo, [message], [channel] ", 0);
        }


    }

    private String getFullname(String userF, String userL) {
        // get user fullname
        String fullname;
        if (userL == null) {
            fullname = userF;
        } else {
            fullname = userF + userL;
        }
        return fullname;

    }

    private void addFullname(long user_id, String user_username, String user_fullname) {
        // add fullname column for those early user
        Document myDoc = collection.find(eq("tg_id", user_id)).first();
        if (myDoc != null) {
            //add fullname if not set yet
            if (myDoc.getString("fullname") == null) {
                Bson updatevalue = new Document("fullname", user_fullname);
                Bson updateDoc = new Document("$set", updatevalue);
                collection.updateOne(myDoc, updateDoc);
                replyUser(admintgId, "Updated user id:" + user_id + "\nFullname:" + user_fullname, 0);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "CountJBot";
    }

    @Override
    public String getBotToken() {
        return apitoken;
    }

    private String getDatebyDay(String inputDate, long channel_id) {
        //get date by a day number
        LocalDate startDate = null;
        String startDateString = "null";
        Pattern dayPattern = Pattern.compile(dayRE);
        Matcher dayMatcher = dayPattern.matcher(inputDate);
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));

        if (dayMatcher.find() && dayMatcher.group().length() != 0) {
            String cmdDate = dayMatcher.group(0);
            String[] splitDate = cmdDate.split("\\s+");
            startDate = currentDate.minusDays(Integer.parseInt(splitDate[1]));
            System.out.println("Input date is: " + startDate);
            startDateString = startDate.toString();
        }
        return startDateString;
    }

    private int getDayNo(long user_id) {
        // get day no by the date stored in database
        Document myDoc = collection.find(eq("tg_id", user_id)).first();
        LocalDate startDate = new LocalDate(myDoc.getString("startDate"));
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));
        return Days.daysBetween(startDate, currentDate).getDays();
    }

    public void getRank() {
        //get rank of database, not done
        LocalDate currentDate = new LocalDate(DateTimeZone.forID("Asia/Hong_Kong"));
        int total = (int) collection.count();
        Document doc = new Document();
        List<Document> allDoc = new ArrayList<Document>();
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> cursor = findIterable.iterator();
        try {
            while (cursor.hasNext()) {
                doc = cursor.next();
                allDoc.add(doc);
            }
        } finally {
            cursor.close();
        }
        for (int a = 0; a < allDoc.size(); a++) {
            System.out.println(allDoc.get(a));
        }


//        collection.find().forEach((Block<Document>) document ->{
//            doc.append("tg_id",document.get("tg_id"))
//                    .append("startDate",document.getString("startDate"));
//            allDoc.add(doc);
//          //System.out.println(document.get("tg_id").toString()+ ","+ document.getString("startDate"));
//        }
//        );
    }

    private void deleteDoc(long user_id, long channel_id, int msgId) {
        Document userDoc = collection.find(eq("tg_id", user_id)).first();
        if (userDoc != null) {
            collection.deleteOne(userDoc);
            replyUser(channel_id, "已經刪除你的記錄", msgId);

        } else {
            replyUser(channel_id, "刪除不成功", msgId);
        }
    }

    private String checkDate(String inputDate, long channel_id, int msgId) {
        //check user input date validation with regular expression
        String correctDate = "false";
        Pattern datePattern = Pattern.compile(dateRE);
        Matcher dateMatcher = datePattern.matcher(inputDate);

        if (dateMatcher.find() && dateMatcher.group().length() != 0) {
            //replyUser(channel_id, "你輸入嘅日期係: " + dateMatcher.group(0));
            correctDate = dateMatcher.group(0);
        } else {
            replyUser(channel_id, "日期格式唔啱,請入多次", msgId);
        }
        return correctDate;
    }

    private LocalDate getDatebyDate(String correctDate, long channel_id, int msgId) {
        // check user input date with joda date calendar
        LocalDate startDate = null;
        // if input date is valid
        if (!correctDate.equals("false")) {
            try {
                startDate = new LocalDate(correctDate);
            } catch (Exception e) {
                if (e.toString().contains("IllegalFieldValueException")) {
                    replyUser(channel_id, correctDate + "? 打到傻左呀? 再入過!", msgId);
                }
            }
            //System.out.println(startDate);
        }
        return startDate;
    }

    private boolean compareDate(LocalDate startDate, LocalDate currentDate) {
        // check if user input date is larger than current date
        if (startDate.isAfter(currentDate)) {
            return false;
        } else {
            return true;
        }
    }


}

