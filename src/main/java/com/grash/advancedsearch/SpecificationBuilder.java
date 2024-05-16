package com.grash.advancedsearch;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class SpecificationBuilder<T> {
    private final List<FilterField> filterFields;

    public SpecificationBuilder() {
        this.filterFields = new ArrayList<>();
    }

    public final SpecificationBuilder<T> with(FilterField filterField) {
        filterFields.add(filterField);
        return this;
    }

    public Specification<T> build() {
        if (CollectionUtils.isEmpty(filterFields)) {
            return null;
        }
        Specification<T> result = (root, query, criteriaBuilder) -> null;
        for (FilterField criteria : filterFields) {
            result = Specification.where(result).and(new WrapperSpecification<>(criteria));
        }
        return result;
    }

}
