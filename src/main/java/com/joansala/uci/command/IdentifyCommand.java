package com.joansala.uci.command;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import static com.joansala.uci.UCI.*;


/**
 * Show information about the engine and its options.
 */
public class IdentifyCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, Parameters params) {
        sendIdentification(service);
        sendOptions(service);
        service.send(UCIOK);
    }


    /**
     * Sends the engine name and author to the client.
     *
     * @param service       UCI service
     */
    private void sendIdentification(UCIService service) {
        Package pack = service.getClass().getPackage();

        String title = pack.getImplementationTitle();
        String version = pack.getImplementationVersion();
        String vendor = pack.getImplementationVendor();

        String name = getValueOrDefault(title, "Unnamed engine");
        String author = getValueOrDefault(vendor, "Unknown author");
        String build = getValueOrDefault(version, "");

        service.send(ID, NAME, name, build);
        service.send(ID, AUTHOR, author);
    }


    /**
     * Sends a list of enabled options to the client.
     *
     * @param service       UCI service
     */
    private void sendOptions(UCIService service) {
        service.getOptions().forEach((key, option) -> {
            if (option.isEnabled()) {
                service.send(OPTION, NAME, key, option);
            }
        });
    }


    /**
     * Get first value if not null or blank, otherwise the fallback value.
     */
    private static String getValueOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
