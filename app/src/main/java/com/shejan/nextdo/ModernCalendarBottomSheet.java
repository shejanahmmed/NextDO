package com.shejan.nextdo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.shejan.nextdo.databinding.BottomSheetCalendarBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ModernCalendarBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetCalendarBinding binding;
    private Calendar currentMonth;
    private Calendar selectedDate;
    private CalendarAdapter adapter;
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(long dateInMillis);
    }

    public static ModernCalendarBottomSheet newInstance(long selectedDateInMillis) {
        ModernCalendarBottomSheet fragment = new ModernCalendarBottomSheet();
        Bundle args = new Bundle();
        args.putLong("SELECTED_DATE", selectedDateInMillis);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.app.Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
            android.widget.FrameLayout bottomSheet = bottomSheetDialog
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);

        selectedDate = Calendar.getInstance();
        if (getArguments() != null) {
            long dateInMillis = getArguments().getLong("SELECTED_DATE", 0);
            if (dateInMillis > 0) {
                selectedDate.setTimeInMillis(dateInMillis);
                currentMonth.setTimeInMillis(dateInMillis);
                currentMonth.set(Calendar.DAY_OF_MONTH, 1);
            }
        }

        setupRecyclerView();
        updateMonthDisplay();
        updateSelectedDateDisplay();

        binding.buttonPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            updateCalendar();
        });

        binding.buttonNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            updateCalendar();
        });

        binding.buttonCancel.setOnClickListener(v -> dismiss());

        binding.buttonOk.setOnClickListener(v -> {
            if (listener != null && selectedDate != null) {
                listener.onDateSelected(selectedDate.getTimeInMillis());
            }
            dismiss();
        });

        // Initial load
        updateCalendar();
    }

    private void setupRecyclerView() {
        binding.recyclerCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        adapter = new CalendarAdapter(new ArrayList<>(), selectedDate, date -> {
            selectedDate = date;
            updateSelectedDateDisplay();
        });
        binding.recyclerCalendar.setAdapter(adapter);
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.textMonthYear.setText(sdf.format(currentMonth.getTime()).toUpperCase());
    }

    private void updateSelectedDateDisplay() {
        if (selectedDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            binding.textSelectedDate.setText(sdf.format(selectedDate.getTime()));
        }
    }

    private void updateCalendar() {
        List<Calendar> days = new ArrayList<>();
        Calendar calendar = (Calendar) currentMonth.clone();

        // Determine the day of the week for the 1st of the month
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Sunday is 1
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add empty placeholders for days before the 1st
        for (int i = 1; i < firstDayOfWeek; i++) {
            days.add(null);
        }

        // Add days of the month
        for (int i = 1; i <= daysInMonth; i++) {
            Calendar day = (Calendar) calendar.clone();
            day.set(Calendar.DAY_OF_MONTH, i);
            days.add(day);
        }

        adapter.updateDays(days);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
