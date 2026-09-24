package com.toabea.closet.common;

import java.time.Instant;
import java.util.Map;

public final class ApiModels {
  private ApiModels() {}
  public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {}
  public record PageResponse<T>(java.util.List<T> content, long totalElements, int page, int size) {}
  public record MessageResponse(String message) {}
}
