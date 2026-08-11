package com.rbac.model.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    private String id;

    private String questionText;

    private List<String> options;

    private int correctOptionIndex;

    private double marks;
}
