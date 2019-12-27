

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreatePatchClass {


    public static String patchFile="E:\\importDir\\20191227";//补丁文件,由eclipse svn plugin生成

    public static String projectPath="E:/iwhale/project/jiheout";//项目文件夹路径

    public static String classPath="target\\classes";//class存放路径

    public static String desPath="E:\\update_pkg";//补丁文件包存放路径

    public static String version="20191227";//补丁版本


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        File file = new File(patchFile);		//获取其file对象
        List<String> fileList=new ArrayList<String>();
        getPatchFileList(fileList,file);
        copyFiles(fileList);
    }

    /***
     * 处理内部类的情况
     * 解析源路径名称，遍历此文件路径下是否存在这个类的内部类
     * 内部类编译后的格式一般是 OuterClassName$InnerClassName.class
     * @param sourceFullFileName 原路径
     * @param desFullFileName 目标路径
     */
    private static void copyInnerClassFile(String sourceFullFileName,String desFullFileName){

        String sourceFileName = sourceFullFileName.substring(sourceFullFileName.lastIndexOf(File.separator)+1);
        String sourcePackPath = sourceFullFileName.substring(0,sourceFullFileName.lastIndexOf(File.separator));
        String destPackPath = desFullFileName.substring(0,desFullFileName.lastIndexOf(File.separator));
        String tempFileName = sourceFileName.split("\\.")[0];
        File packFile = new File(sourcePackPath);
        if(packFile.isDirectory()){
            String[] listFiles = packFile.list();
            for(String fileName:listFiles){
                //可以采用正则表达式处理
                if(fileName.indexOf(tempFileName+"$")>-1 && fileName.endsWith(".class")){
                    String newSourceFullFileName = sourcePackPath+File.separator+fileName;
                    String newDesFullFileName = destPackPath + File.separator + fileName;
                    copyFile(newSourceFullFileName, newDesFullFileName);
                    System.out.println(newSourceFullFileName+"复制完成");
                }
            }
        }

    }

    /****
     * 读取补丁配置文件解析出修改的文件并返回到list集合
     * @return
     * @throws Exception
     */
    public static void getPatchFileList(List<String> fileList, File file) throws Exception{
        File[] fs = file.listFiles();	//遍历path下的文件和目录，放在File数组中
        for(File f:fs){					//遍历File[]数组
            if(f.isDirectory())	//若是目录，则递归打印该目录下的文件
                getPatchFileList(fileList,f);
            if(f.isFile())
                fileList.add(f.getPath());
        }

    }

    /***
     *
     * @param list 修改的文件
     */
    public static void copyFiles(List<String> list){

        for(String fullFileName:list){
            if(fullFileName.indexOf("src\\main\\java\\")!=-1){//对源文件目录下的文件处理
                String fileName=fullFileName.replace("src\\main\\java\\","");
                String classes = null;
                String xdlj = fileName.substring(fileName.indexOf("audit"));
                String subProject=xdlj.substring(0,xdlj.indexOf(File.separator)) ;
                classes = projectPath+File.separator+subProject+File.separator+classPath+File.separator+fullFileName.substring(fullFileName.indexOf("\\com"));
//                fullFileName=classPath+fileName;
                if(fullFileName.endsWith(".java")){
                    classes=classes.replace(".java",".class");
//                    fullFileName=fullFileName.replace(".java",".class");
                }
                String tempDesPath=fileName.substring(0,fileName.lastIndexOf(File.separator));
                String desFilePathStr=desPath+"/"+version+File.separator+tempDesPath.substring(tempDesPath.indexOf("audit"));
                String desFileNameStr=desFilePathStr+classes.substring(classes.lastIndexOf(File.separator));
                File desFilePath=new File(desFilePathStr);
                if(!desFilePath.exists()){
                    desFilePath.mkdirs();
                }
                copyFile(classes, desFileNameStr);
                copyInnerClassFile(classes, desFileNameStr);
                System.out.println(classes+"复制完成");
            }else{//对普通目录的处理
                String desFileName=fullFileName.replace("src\\main\\","");
                fullFileName=projectPath+"/"+fullFileName.substring(fullFileName.indexOf("audit"));//将要复制的文件全路径
                String fullDesFileNameStr=desPath+"/"+version+File.separator+desFileName.substring(desFileName.indexOf("audit"));
                String desFilePathStr=fullDesFileNameStr.substring(0,fullDesFileNameStr.lastIndexOf(File.separator));
                File desFilePath=new File(desFilePathStr);
                if(!desFilePath.exists()){
                    desFilePath.mkdirs();
                }
                copyFile(fullFileName, fullDesFileNameStr);
                System.out.println(fullFileName+"复制完成");

            }

        }

    }

    private static void copyFile(String sourceFileNameStr, String desFileNameStr) {
        File srcFile=new File(sourceFileNameStr);
        File desFile=new File(desFileNameStr);
        try {
            copyFile(srcFile, desFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            // 新建文件输入流并对它进行缓冲
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            // 新建文件输出流并对它进行缓冲
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            // 缓冲数组
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();
        } finally {
            // 关闭流
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }
}
