package com.traffbooster.car.core;

import java.util.List;

public interface ISuccessListener {
    void success(List<Boolean> result);
    void failed();
}
