package lecture_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 講師ホーム用：月間カレンダーの1マス */
@Data
public class CalendarCellDto {
    private LocalDate date;
    private boolean inCurrentMonth;
    private List<InstructorScheduleRowDto> lessons = new ArrayList<>();
}
