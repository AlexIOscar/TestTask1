import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static final Logger console_logger;
    public static final Logger file_logger;

    static {
        console_logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        file_logger = LoggerFactory.getLogger(Main.class);
    }

    public static void main(String[] args) {
        console_logger.info("logging started");
        strRepl("C:\\testDir", "dolor", "lalala");
    }

    /**
     * Replace all target-sequences(oldSeq) with new sequence (newSeq) in all files from subdirectories of path
     *
     * @param path   the target directory
     * @param oldSeq the sequence to be replaced
     * @param newSeq the replacement sequence
     */
    @SuppressWarnings("ConstantConditions")
    public static void strRepl(String path, String oldSeq, String newSeq) {

        //debug tool. Should switch it to "false" to work
        boolean testMode = false;

        //the qty of signs before and after target in the log
        int overlap = 6;

        List<Path> paths = null;

        //File list creation
        try {
            paths = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .peek(s -> console_logger.trace("{} file detected", s.toString()))
                    .collect(Collectors.toList());
        } catch (IOException ioException) {
            file_logger.error("something wrong with file list creation");
            ioException.printStackTrace();
        }

        if (paths != null) {
            //for each file in the list...
            for (Path p : paths) {
                String fileName = String.valueOf(p);
                File f = new File(fileName);
                String inputText = null;
                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(new FileInputStream(f), "CP1251"))) {
                    StringBuilder text = new StringBuilder();
                    String str = br.readLine();
                    while (str != null) {
                        text.append(str);
                        str = br.readLine();
                    }
                    inputText = text.toString();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    file_logger.error("File reading err: " + p.toString());
                }

                if (inputText == null) {
                    continue;
                }

                Pattern regex = Pattern.compile("((?<=.)" + oldSeq + "(?=.))|(^" + oldSeq + ")|(" + oldSeq + "$)");
                Matcher matcher = regex.matcher(inputText);

                //The map of match's indexes
                List<Integer> posMap = new ArrayList<>();

                while (matcher.find()) {
                    posMap.add(matcher.start());
                    file_logger.trace("match in position: {}", matcher.start());
                }

                //nothing to replace
                if (posMap.size() == 0) {
                    continue;
                }

                file_logger.info("Match(es) detected in {}", p.toString());

                String out = matcher.replaceAll(newSeq);
                //System.out.println(out);
                file_logger.info(getLogBlock(inputText, posMap, oldSeq, newSeq, overlap));

                try (BufferedWriter bw =
                             new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "CP1251"))) {
                    bw.write(testMode ? inputText : out);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    file_logger.error("File writing err: {}", p.toString());
                }
            }
        }
    }

    /**
     * @param source   input text to be processed
     * @param indexMap indexes of matches starts
     * @param target   the sequence to be replaced
     * @param repl     the replacement sequence
     * @param overlap  the width overlap before and after replacement block (for log line)
     * @return general info block about replacements for logging
     */
    /*
    TODO: Не очень хорошее решение: замена в итоге за всю программу производится дважды, один раз в основной части, и
     один раз - для лога. Лучше (но немного сложнее) вычислить смещения индексов в файле-замене, и вытаскивать
     подстроки непосредственно из него (передавая его как еще один  аргумент).
     */
    public static String getLogBlock(String source, List<Integer> indexMap, String target, String repl, int overlap) {
        StringBuilder sb = new StringBuilder("Replacements:\n");
        for (Integer index : indexMap) {
            String before = source.substring(index - overlap, index + target.length() + overlap);
            String after = before.replaceAll(target, repl);
            sb.append(before).append(" -> ").append(after).append(" (position: ").append(index).append(")").append('\n');
        }
        return sb.toString();
    }
}