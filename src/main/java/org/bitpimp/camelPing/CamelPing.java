package org.bitpimp.camelPing;

import org.apache.camel.main.Main;

/**
 * A Camel Application
 */
public class CamelPing {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.enableHangupSupport();
        main.addRouteBuilder(new MyRouteBuilder(args));
        // No camel main options, all args consumed by the route builder
        main.run(new String[0] /* args */);
    }

}

