package org.jenkinsci.plugins.visualworks_store;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.util.IOException2;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreChangeLogParser extends ChangeLogParser {
    private List<StoreChangeLogEntry> entries;
    private Map<String, StoreChangeLogEntry> blessingMap;

    @Override
    public StoreChangeLogSet parse(AbstractBuild build, File file) throws IOException, SAXException {
        entries = new ArrayList<StoreChangeLogEntry>();
        blessingMap = new HashMap<String, StoreChangeLogEntry>();

        SAXReader reader = new SAXReader();

        try {
            Document document = reader.read(file);
            Element root = document.getRootElement();
            for (Object eachPundle : root.elements("package")) {
                Element pundle = (Element) eachPundle;
                List blessings = pundle.elements("blessing");

                ChangedPundle changedPundle = new ChangedPundle(pundle.attributeValue("action"),
                        pundle.attributeValue("name"), pundle.attributeValue("version"));

                if (blessings.isEmpty()) {
                    newDeletion(changedPundle);
                    continue;
                }

                for (Object eachBlessing : blessings) {
                    Element blessing = (Element) eachBlessing;
                    newEntry(blessing.attributeValue("user"),
                            blessing.attributeValue("timestamp"),
                            blessing.getText().trim(), changedPundle);
                }
            }
        } catch (DocumentException e) {
            throw new IOException2("Failed to parse changelog file: ", e);
        }
        return new StoreChangeLogSet(build, entries);
    }

    private void newDeletion(ChangedPundle pundle) {
        newEntry("", null, "Pundles no longer used", pundle);
    }

    private void newEntry(String committer, String timestamp, String blessingComment, ChangedPundle pundle) {
        StoreChangeLogEntry entry = blessingMap.get(blessingComment);
        if (entry == null) {
            entry = new StoreChangeLogEntry(committer, timestamp, blessingComment);
            entries.add(entry);
            blessingMap.put(blessingComment, entry);
        }
        entry.updateTimestamp(timestamp);
        entry.addPundle(pundle);
    }
}
