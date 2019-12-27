import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class SvnCodeVersionDownload {

    private static String url = null;
    private static String name = null;
    private static String password = null;
    private static String exportPath = null;
    private static String localPath = null;
    private static SVNURL repositoryURL = null;
    private static SVNRepository repository = null;
    private static SVNClientManager ourClientManager = null;
    private static SVNUpdateClient updateClient = null;

    public SvnCodeVersionDownload() {
        InputStream inStream = SvnCodeVersionDownload.class.getClassLoader()
                .getResourceAsStream("env.properties");
        Properties prop = new Properties();
        try {
            prop.load(inStream);
        } catch (IOException e) {
            System.err.println("读取env.properties 出错" + "': " + e.getMessage());
            System.exit(1);
        }
        url = prop.getProperty("svnUrl");//"https://192.168.30.32/svn/R/r_erp/源码";
        name = prop.getProperty("svnUser");
        password = prop.getProperty("svnPwd");
        exportPath = prop.getProperty("exportPath");
        localPath = prop.getProperty("workPath");
//		String logpath = exportPath+patch+File.separator+"filelist.log";
    }

    public void execDownload(List<Integer> version) {
        StringBuffer sb = new StringBuffer();
        Calendar ca = Calendar.getInstance();
        StringBuffer year = new StringBuffer("" + ca.get(Calendar.YEAR) + (ca.get(Calendar.MONTH) + 1) + ca.get(Calendar.DATE));
        File wcDir = new File(exportPath + File.separator + year);
        if (wcDir.exists())
            wcDir.delete();
        //判断传入版本号
        if (version == null || version.size() == 0) {
            sb.append("版本号不能为空！");
        } else {
            //首先处理版本号
            for (int i = 0; i < version.size() - 1; i++) {
                for (int j = 1; j < version.size() - i; j++) {
                    Integer a;
                    if ((version.get(j - 1)).compareTo(version.get(j)) > 0) {   //比较两个整数的大小
                        a = version.get(j - 1);
                        version.set((j - 1), version.get(j));
                        version.set(j, a);
                    }
                }
            }
            int len = version.size();

            DAVRepositoryFactory.setup();
            try {
                repositoryURL = SVNURL.parseURIEncoded(url);
                repository = SVNRepositoryFactory.create(repositoryURL);

                ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
                //实例化客户端管理类
                ourClientManager = SVNClientManager.newInstance((DefaultSVNOptions) options, name, password);
                //要把版本库的内容check out到的目录
                //通过客户端管理类获得updateClient类的实例。
                updateClient = ourClientManager.getUpdateClient();
                updateClient.setIgnoreExternals(false);

                ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
                repository.setAuthenticationManager(authManager);
                for (int i = 0; i < len; i++) {
                    sb.append(listEntries("", version.get(i), year.toString()));
                }
            } catch (SVNException svne) {
                System.err.println("创建版本库实例时失败，版本库的URL是 '" + url + "': " + svne.getMessage());
                System.exit(1);
            }

            long latestRevision = -1;
            try {
                latestRevision = repository.getLatestRevision();
            } catch (SVNException svne) {
                System.err
                        .println("获取最新版本号时出错: "
                                + svne.getMessage());
                System.exit(1);
            }
        }

        //写日志
        if (sb.length() > 0) {
            File file = new File(exportPath + File.separator + year + ".log");
            //if(!file.exists()) file.mkdir();
            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                writer.write(sb.toString());
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /*
     * 此函数递归的获取版本库中某一目录下的所有条目。
     */
    public static String listEntries(String path, int version, String year)
            throws SVNException {
        StringBuffer sb = new StringBuffer();
        //获取版本库的path目录下的所有条目。参数－1表示是最新版本。

        Collection entries = repository.getDir(path, version, null,
                (Collection) null);
        Iterator iterator = entries.iterator();
        int size = 0;
        while (iterator.hasNext()) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            if (entry.getRevision() == version) {
                if (entry.getKind() == SVNNodeKind.FILE) {
                    System.out.println("/" + (path.equals("") ? "" : path + "/")
                            + entry.getName());
                    sb.append("第" + (size + 1) + "个版本号" + version + "开始下载->>>>\n");
                    SVNURL u = SVNURL.parseURIEncoded(repositoryURL + "/" + path + "/" + entry.getName());
                    File fi = new File(exportPath + File.separator + year + "/" + path + "/" + entry.getName());
                    long workingVersion = 0;
                    try {
                        workingVersion = updateClient.doExport(u, fi, SVNRevision.HEAD, SVNRevision.parse(version + ""), "native", true, false);
                        //workingVersion = updateClient.doCheckout(u, fi, SVNRevision.HEAD, SVNRevision.parse(version+""), SVNDepth.INFINITY,false);
                    } catch (SVNException e) {
                        e.printStackTrace();
                    }
                    System.out.println("把版本：" + workingVersion + " check out 到目录：" + fi + "中。");
                    sb.append("第" + (size + 1) + "个版本号" + version + "结束下载-<<<<\n");
                    size++;
                }
                /*
                 * 检查此条目是否为目录，如果为目录递归执行
                 */
                if (entry.getKind() == SVNNodeKind.DIR) {
                    listEntries((path.equals("")) ? entry.getName()
                            : path + "/" + entry.getName(), version, year);
                }
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        List<Integer> version = new ArrayList<Integer>();
        version.add(58377);
        version.add(58058);
        version.add(57364);
        //version.add(e);
        //version.add(e);

        SvnCodeVersionDownload rt = new SvnCodeVersionDownload();
        rt.execDownload(version);
    }
}
