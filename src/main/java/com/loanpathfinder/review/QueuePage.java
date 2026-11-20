package com.loanpathfinder.review;

import java.util.List;

public record QueuePage(List<QueueRow> rows, int page, long total) {
}
