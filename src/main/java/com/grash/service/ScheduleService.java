package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.Task;
import com.grash.model.WorkOrder;
import com.grash.repository.ScheduleRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final PreventiveMaintenanceService preventiveMaintenanceService;
    private final ScheduleMapper scheduleMapper;
    private final WorkOrderService workOrderService;
    private final TaskService taskService;

    private Map<Long, Timer> timers = new HashMap<>();

    public Schedule create(Schedule Schedule) {
        return scheduleRepository.save(Schedule);
    }

    public Schedule update(Long id, SchedulePatchDTO schedule) {
        if (scheduleRepository.existsById(id)) {
            Schedule savedSchedule = scheduleRepository.findById(id).get();
            return scheduleRepository.save(scheduleMapper.updateSchedule(savedSchedule, schedule));
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<Schedule> getAll() {
        return scheduleRepository.findAll();
    }

    public void delete(Long id) {
        scheduleRepository.deleteById(id);
    }

    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    public Collection<Schedule> findByCompany(Long id) {
        return scheduleRepository.findByCompany_Id(id);
    }

    public void scheduleWorkOrder(Schedule schedule) {
        boolean shouldSchedule = !schedule.isDisabled() && (schedule.getEndsOn() == null || schedule.getEndsOn().after(new Date()));
        if (shouldSchedule) {
            Timer timer = new Timer();
            //  Collection<WorkOrder> workOrders = workOrderService.findByPM(schedule.getPreventiveMaintenance().getId());
            Date startsOn = Helper.getNextOccurence(schedule.getStartsOn(), schedule.getFrequency());
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    PreventiveMaintenance preventiveMaintenance = schedule.getPreventiveMaintenance();
                    WorkOrder workOrder = workOrderService.getWorkOrderFromWorkOrderBase(preventiveMaintenance);
                    Collection<Task> tasks = taskService.findByPreventiveMaintenance(preventiveMaintenance.getId());
                    workOrder.setParentPreventiveMaintenance(schedule.getPreventiveMaintenance());
                    if (schedule.getDueDateDelay() != null) {
                        workOrder.setDueDate(Helper.incrementDays(new Date(), schedule.getDueDateDelay()));
                    }
                    WorkOrder savedWorkOrder = workOrderService.create(workOrder);
                    tasks.forEach(task -> {
                        Task copiedTask = new Task(task.getTaskBase(), savedWorkOrder, null, task.getValue());
                        taskService.create(copiedTask);
                    });
                    workOrderService.notify(savedWorkOrder, Helper.getLocale(workOrder.getCompany()));
                }
            };
            timer.scheduleAtFixedRate(timerTask, startsOn, (long) schedule.getFrequency() * 24 * 60 * 60 * 1000);
            timers.put(schedule.getId(), timer);
            if (schedule.getEndsOn() != null) {
                Timer timer1 = new Timer();
                TimerTask timerTask1 = new TimerTask() {
                    @Override
                    public void run() {
                        timers.get(schedule.getId()).cancel();
                        timers.get(schedule.getId()).purge();
                    }
                };
                timer1.schedule(timerTask1, schedule.getEndsOn());
            }
        }
    }

    public void reScheduleWorkOrder(Long id, Schedule schedule) {
        timers.get(id).cancel();
        timers.get(id).purge();
        scheduleWorkOrder(schedule);
    }

    public Schedule save(Schedule schedule) {
        return scheduleRepository.saveAndFlush(schedule);
    }
}
