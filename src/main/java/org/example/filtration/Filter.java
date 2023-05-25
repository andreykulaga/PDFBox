package org.example.filtration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class Filter {
    private Operator operator;
    private String value;

    public enum Operator {
        eq,
        neq,
        contains,
        doesnotcontain,
        beginswith,
        endswith,
        isempty,
        isnotempty,
        gt,
        gte,
        lt,
        lte,
        isnull,
        isnotnull
    }
}

