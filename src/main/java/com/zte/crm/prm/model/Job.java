package com.zte.crm.prm.model;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class Job {
    private String jobName="test init expr";
    @Autowired
    private Date jobDate=new Date(1987L);
}
