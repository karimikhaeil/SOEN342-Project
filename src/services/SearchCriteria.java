package services;

import models.TaskStatus;
import java.time.LocalDate;

public class SearchCriteria {
    private LocalDate startDate;
    private LocalDate endDate;
    private String titleKeyword;
    private TaskStatus status;
    private String dayOfWeek;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getTitleKeyword() { return titleKeyword; }
    public void setTitleKeyword(String titleKeyword) { this.titleKeyword = titleKeyword; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
