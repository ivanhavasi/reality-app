package cz.havasi.reality.app.service

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.sync.Mutex

// scheduled scrapes and admin jobs hit the same portals, so they never run at the same time
@ApplicationScoped
public class PortalLock {
    public val mutex: Mutex = Mutex()
}
