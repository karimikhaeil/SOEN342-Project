package services;

import models.TaskStatus;
import java.time.LocalDate;

public class SearchCriteria {
    private LocalDate startDate;
    private LocalDate endDate;
    private String titleKeyword;
    private TaskStatus status;
    private String dayOfWeek; // e.g. "MONDAY"

    public LocalDate getStartDate()       { return startDate; }
    public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate()         { return endDate; }
    public void setEndDate(LocalDate d)   { this.endDate = d; }
    public String getTitleKeyword()       { return titleKeyword; }
    public void setTitleKeyword(String k) { this.titleKeyword = k; }
    public TaskStatus getStatus()         { return status; }
    public void setStatus(TaskStatus s)   { this.status = s; }
    public String getDayOfWeek()          { return dayOfWeek; }
    public void setDayOfWeek(String d)    { this.dayOfWeek = d; }
}
