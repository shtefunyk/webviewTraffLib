package com.aff2.car.core;

import java.util.List;

public interface ISuccessListener {
    void success(List<Boolean> result);
    void failed();
}
