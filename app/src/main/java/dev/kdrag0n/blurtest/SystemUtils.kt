package dev.kdrag0n.blurtest

import com.topjohnwu.superuser.Shell

fun systemBoost() {
    // Lock GPU and CPU frequencies, affine to prime cluster, freeze power HAL
    Shell.su(
        "sleep 1",
        """
            pwr_pid="$(ps -A | grep libperfmgr | awk '{print ${'$'}2}')"
            echo "${'$'}pwr_pid" > /sys/fs/cgroup/freezer/cgroup.procs
        """.trimIndent(),
        "sleep 0.25",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on",
        "echo 20000 > /sys/class/kgsl/kgsl-3d0/idle_timer",
        "echo 500000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "echo 500000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/min_freq",
        "echo 500000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/max_freq",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor",
        "sleep 2",
        """
            main_pid="$(ps -A | grep dev.kdrag0n.blurtest | awk '{print ${'$'}2}')"
            for p in $(ls "/proc/${'$'}main_pid/task")
            do
                taskset -p 80 ${'$'}p
                chrt -fp ${'$'}p 10
            done
        """.trimIndent()
    ).submit()
}

fun systemUnboost() {
    Shell.su(
        "echo 3 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on",
        "echo 80 > /sys/class/kgsl/kgsl-3d0/idle_timer",
        "echo 625000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "echo 275000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/min_freq",
        "echo 625000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/max_freq",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor",
        """
            pwr_pid="$(ps -A | grep libperfmgr | awk '{print ${'$'}2}')"
            echo "${'$'}pwr_pid" > /sys/fs/cgroup/cgroup.procs
        """.trimIndent(),
        """
            main_pid="$(ps -A | grep dev.kdrag0n.blurtest | awk '{print ${'$'}2}')"
            for p in $(ls "/proc/${'$'}main_pid/task")
            do
                taskset -p ff ${'$'}p
                chrt -op ${'$'}p 0
            done
        """.trimIndent()
    ).submit()
}