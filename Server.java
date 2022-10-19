package screen_share;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import sun.awt.image.codec.JPEGImageEncoderImpl;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Author:tang
 * Date:2022/9/20 14:08
 */
public class Server {
    public static void main(String[] args) {
        try {
            //指定监控的端口号
            ServerSocket serverSocket = new ServerSocket(10001);
            System.out.println("正在等待连接。。。");
            //获取连接
            Socket accept = serverSocket.accept();
            System.out.println("已连接！");
            //获取输入输出流
            OutputStream outputStream = accept.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            InputStream inputStream = accept.getInputStream();
            DataInputStream dataInputStream=new DataInputStream(inputStream);
            //开启一个线程发送图片
            SendData sendData = new SendData(dataOutputStream);
            sendData.start();
            //开启一个线程获取 鼠标 事件
            MouseEventThread mouseEventThread =new MouseEventThread(dataInputStream);
            mouseEventThread.start();
            //开启一个线程获取 键盘 事件
            KeyboardEventThread keyboardEventThread = new KeyboardEventThread(10002);
            keyboardEventThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 发送监控画面线程
 */
class SendData extends Thread{
    //向哪一个客户端发送数据
    private DataOutputStream dataOutputStream;

    public SendData(DataOutputStream dataOutputStream) {
        this.dataOutputStream = dataOutputStream;
    }
    /**
     * 实际运行逻辑
     */
    @Override
    public void run() {
        //获取屏幕分辨率
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        //开始发送数据
        try {
            /**
             * 发送屏幕分辨率到客户端
             */
            dataOutputStream.writeDouble(screenSize.getWidth());
            dataOutputStream.writeDouble(screenSize.getHeight());
            dataOutputStream.flush();
            /**
             * 定义截图的区域
             * 这里为全屏
             */
            Rectangle ret=new Rectangle((int)screenSize.getWidth(),(int)screenSize.getHeight());
            //创建机器人
            Robot robot=new Robot();
            //持续发送图片数据
           while(true){
               //机器人截图
               BufferedImage screenImage = robot.createScreenCapture(ret);
               //开始压缩图片
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               JPEGImageEncoder encoder=new JPEGImageEncoderImpl(baos);
               encoder.encode(screenImage);
               //发送图片
               byte[] bytes = baos.toByteArray();
               dataOutputStream.writeInt(bytes.length);
               dataOutputStream.write(bytes);
               dataOutputStream.flush();
               Thread.sleep(50);
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * 获取客户端鼠标事件并执行的线程
 */
class MouseEventThread extends Thread{
    private DataInputStream dataInputStream;//用于读取鼠标xy轴数据
    private int mouseX;//鼠标移动的X轴
    private int mouseY;//鼠标移动的Y轴

    public MouseEventThread(DataInputStream dataInputStream) {
        this.dataInputStream = dataInputStream;
    }

    /**
     * 线程执行逻辑
     */
    @Override
    public void run() {
        Robot robot=null;//用于模拟人的行为操控电脑
        try {
             robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        //循环读取数据
        while (true){
            try {
                mouseX=dataInputStream.readInt();
                if(mouseX>=0){
                    mouseMoved(robot,mouseX);
                }else {
                    judgeOperation(mouseX,robot);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 鼠标移动事件
     */
    public void mouseMoved(Robot robot,int mouseX)  {
        try {
            mouseY=dataInputStream.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        robot.mouseMove((int)(mouseX),(int)(mouseY));
    }

    /**
     * 鼠标左键按下
     */
    public void mouseLeftDown(Robot robot){
        robot.mousePress(KeyEvent.BUTTON1_MASK);
    }
    /**
     * 鼠标左键弹起
     */
    public void mouseLeftUp(Robot robot){
        robot.mouseRelease(KeyEvent.BUTTON1_MASK);
    }

    /**
     * 鼠标右键按下
     */
    public void mouseRightDown(Robot robot){
        robot.mousePress(KeyEvent.BUTTON3_MASK);
    }
    /**
     * 鼠标右键弹起
     */
    public void mouseRightUp(Robot robot){
        robot.mouseRelease(KeyEvent.BUTTON3_MASK);
    }
    /**
     * 鼠标下滑滑轮
     */
    public void mouseWhileDown(Robot robot){
        robot.mouseWheel(1);
    }
    /**
     * 鼠标上滑滑轮
     */
    public void mouseWhileUp(Robot robot){
        robot.mouseWheel(-1);
    }
    /**
     * 鼠标左键单击
     */
    public void mouseClicked(Robot robot) {
        robot.mousePress(KeyEvent.BUTTON1_MASK);
        robot.mouseRelease(KeyEvent.BUTTON1_MASK);
    }
    /**
     * 判断应该执行的操作
     */
    public void judgeOperation(int num,Robot robot){
        switch (num){
            case -11:
                robot.delay(500);//以免造成双击
                mouseClicked(robot);//鼠标单击
                break;
            case -12:
                mouseLeftDown(robot);break;//鼠标左键按下
            case -21:
                mouseLeftUp(robot);break;//鼠标左键弹起
            case -13:
                mouseRightDown(robot);break;//鼠标右键按下
            case -31:
                mouseRightUp(robot);break;//鼠标右键弹起
            case -6:
                mouseWhileDown(robot);break;//鼠标滚轮下滑
            case -9:
                mouseWhileUp(robot);//鼠标滚轮上滑

        }

    }
}

/**
 * 键盘事件监听线程
 */
class KeyboardEventThread extends Thread{
    private ServerSocket serverSocket;
    private DataInputStream dataInputStream;
    private Socket socket;
    private int port;

    public KeyboardEventThread(int port) {
        this.port = port;
        getServerSocket();

    }

    private void getServerSocket(){
        try {
            this.serverSocket=new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getDataInputStream(){
        try {
            InputStream inputStream = this.socket.getInputStream();
            this.dataInputStream = new DataInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //监听端口
        try {
            this.socket = serverSocket.accept();
            System.out.println("键盘已连接！");
        } catch (IOException e) {
            e.printStackTrace();
        }
        getDataInputStream();
        //开始读取数据监控鼠标事件
        Robot robot=null;
        try {
             robot=new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                int i = dataInputStream.readInt();
                if(i==-1){
                    executeKeyboardEventDown(dataInputStream.readInt(),robot);
                }else if (i==-2){
                    executeKeyboardEventUp(dataInputStream.readInt(),robot);
                }else {
                    executeKeyboardEventDown(i,robot);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //执行键盘按下操作
    private void executeKeyboardEventDown(int keyCode,Robot robot){
        System.out.println("键盘按下："+(char)keyCode);
        robot.keyPress(keyCode);
    }
    //执行键盘弹起操作
    private void executeKeyboardEventUp(int keyCode,Robot robot){
        System.out.println("键盘弹起："+(char)keyCode);
        robot.keyRelease(keyCode);
    }
}
