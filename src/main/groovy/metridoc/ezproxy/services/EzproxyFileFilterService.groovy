/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.ezproxy.services

import groovy.util.logging.Slf4j
import metridoc.core.InjectArg
import metridoc.core.InjectArgBase
import metridoc.service.gorm.GormService
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter

/**
 * @author Tommy Barker
 */
@Slf4j
@InjectArgBase("ezproxy")
class EzproxyFileFilterService implements GenericFileFilter {

    File file
    boolean preview
    boolean stacktrace
    @InjectArg(ignore = true)
    Class entityClass

    GormService gormService

    @Override
    boolean accept(GenericFile file) {
        try {
            if(this.file) {
                return file.fileNameOnly == this.file.name
            }

            if(preview) return true

            assert entityClass && gormService : "entityClass and gormService must not be null"

            log.debug "testing if file [$file.fileNameOnly] should be processed"
            List result
            entityClass.withTransaction {
                result = entityClass.findAllByFileName(file.fileName)
            }

            boolean processFile = result == null || result.size() == 0

            if(processFile) {
                log.debug "file [$file.fileNameOnly] should be processed for loading table [$entityClass]"
            } else {
                log.debug "file [$file.fileNameOnly] should NOT be processed for loading table [$entityClass]"
            }

            return processFile
        }
        catch (Throwable throwable) {
            if(stacktrace) {
                throwable.printStackTrace()
            } else {
                log.error throwable.message
            }
            return false
        }
    }
}
