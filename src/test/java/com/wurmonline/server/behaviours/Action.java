package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;

public class Action {

    private long subjectId;

    public Action(long subjectId) {
        this.subjectId = subjectId;
    }

    public long getSubjectId() {
        return subjectId;
    }
}
