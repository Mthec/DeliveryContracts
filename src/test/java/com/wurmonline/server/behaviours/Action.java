package com.wurmonline.server.behaviours;


public class Action {

    public long subjectId;

    public Action(long subjectId) {
        this.subjectId = subjectId;
    }

    public long getSubjectId() {
        return subjectId;
    }
}
