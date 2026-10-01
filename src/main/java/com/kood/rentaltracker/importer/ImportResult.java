package com.kood.rentaltracker.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportResult {

    private int inserted;
    private int matched;
    private int rejected;

    private final List<String> rejectionDetails =
            new ArrayList<>();

    public void incrementInserted() {
        inserted++;
    }

    public void incrementMatched() {
        matched++;
    }

    public void incrementRejected() {
        rejected++;
    }

    public void addRejection(
            String file,
            int line,
            String reason
    ) {
        rejected++;

        rejectionDetails.add(
                file
                        + " line "
                        + line
                        + ": "
                        + reason
        );
    }

    public int getInserted() {
        return inserted;
    }

    public int getMatched() {
        return matched;
    }

    public int getRejected() {
        return rejected;
    }

    public List<String> getRejectionDetails() {
        return Collections.unmodifiableList(
                rejectionDetails
        );
    }
}
