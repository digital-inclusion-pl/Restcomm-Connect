/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2018, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.core.service.recording;

import akka.actor.ActorSystem;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.amazonS3.S3AccessTool;
import org.restcomm.connect.core.service.api.RecordingService;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.RecordingsDao;
import org.restcomm.connect.dao.entities.Recording;

import java.io.File;
import java.net.URI;

public class RecordingsServiceImpl implements RecordingService {

    private static Logger logger = Logger.getLogger(RecordingsServiceImpl.class);

    private final DaoManager daoManager;
    private final S3AccessTool s3AccessTool;
    private final ActorSystem actorSystem;

    public RecordingsServiceImpl (DaoManager daoManager, ActorSystem system) {
        this.daoManager = daoManager;
        s3AccessTool = daoManager.getRecordingsDao().getS3AccessTool();
        this.actorSystem = system;
    }

    @Override
    public void removeRecording (Recording recording) {
        RecordingsDao recordingsDao = daoManager.getRecordingsDao();

        boolean isStoredAtS3 = recording.getS3Uri() != null;

        if (isStoredAtS3) {
            if (s3AccessTool != null) {
                s3AccessTool.removeS3Uri(recording.getS3Uri());
            }
        } else {
            URI fileUri = recording.getFileUri();
            if (fileUri.getScheme().equalsIgnoreCase("file")) {
                File recordingFile = new File(fileUri);
                if (recordingFile.exists()) {
                    recordingFile.delete();
                }
            }
        }

        recordingsDao.removeRecording(recording.getSid());

        recording.setDateRemoved(DateTime.now());
        actorSystem.eventStream().publish(recording);
    }

}