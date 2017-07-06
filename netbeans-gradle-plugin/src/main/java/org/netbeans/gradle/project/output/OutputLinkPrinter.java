package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jtrim2.utils.ExceptionHelper;
import org.openide.windows.IOColorPrint;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class OutputLinkPrinter implements SmartOutputHandler.Consumer {
    private final OutputLinkFinder[] linkFinders;

    public OutputLinkPrinter(OutputLinkFinder... linkFinders) {
        this.linkFinders = linkFinders.clone();

        ExceptionHelper.checkNotNullElements(this.linkFinders, "linkFinders");
    }

    private void findLinkDefs(String line, int startIndex, int endIndex, List<OutputLinkDef> linkDefs) {
        if (startIndex >= endIndex || startIndex >= line.length()) {
            return;
        }

        String subStr = line.substring(startIndex, endIndex);
        for (OutputLinkFinder linkFinder: linkFinders) {
            OutputLinkDef linkDef = linkFinder.tryFindLink(subStr);
            // Empty links are unreasonable and may cause an infinite recursion.
            if (linkDef != null && !linkDef.isEmptyLink()) {
                OutputLinkDef baseLinkDef = linkDef.offsetLinkDef(startIndex);

                findLinkDefs(line, startIndex, baseLinkDef.getStartIndex(), linkDefs);
                linkDefs.add(baseLinkDef);
                findLinkDefs(line, baseLinkDef.getEndIndex(), endIndex, linkDefs);
                return;
            }
        }
    }

    private List<OutputLinkDef> findLinkDefs(String line) {
        // Note that in the majority of cases, the line is not a link, so we
        // spare creating a list when not needed.
        for (OutputLinkFinder linkFinder: linkFinders) {
            if (linkFinder.tryFindLink(line) != null) {
                List<OutputLinkDef> result = new ArrayList<>(linkFinders.length);
                findLinkDefs(line, 0, line.length(), result);
                return result;
            }
        }
        return null;
    }

    @Override
    public boolean tryConsumeLine(String line, InputOutput ioParent, OutputWriter output) throws IOException {
        List<OutputLinkDef> linkDefs = findLinkDefs(line);

        if (linkDefs == null) {
            return false;
        }

        int prevEndIndex = 0;
        for (OutputLinkDef linkDef: linkDefs) {
            int startIndex = linkDef.getStartIndex();
            int endIndex = linkDef.getEndIndex();

            if (prevEndIndex < startIndex) {
                output.print(line.substring(prevEndIndex, startIndex));
            }

            String link = line.substring(startIndex, endIndex);
            IOColorPrint.print(ioParent, link, linkDef.toOutputListener(), false, null);

            prevEndIndex = endIndex;
        }

        output.println(line.substring(prevEndIndex));

        return true;
    }
}
