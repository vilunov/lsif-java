package com.sourcegraph.lsifjava;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;

public class Main {
    public static void main(String[] args) {
		Sample.nom();
		
		List<List<String>[][]> listsOfStuff = new ArrayList<List<String>[][]>();

        try(Tracer tracer = new MockTracer()) {
            Span builder = tracer.buildSpan("test").start();
            builder.setTag("hello", "sourcegraph");

            File f = new File("lawl");
			System.out.println(f);
			
			Object o = new Object() {};
        }
	}
}
