import javax.sound.midi.*;

public class Musica {

    private Sequencer sequencer;
    private boolean activa = true;

    // Notas del Korobeiniki (tema A del Tetris) en MIDI
    // Formato: {nota, duracion} donde duracion: 4=negra, 8=corchea, 2=blanca
    private static final int[][] MELODIA = {
            {64,4},{59,8},{60,8},{62,4},{60,8},{59,8},
            {57,4},{57,8},{60,8},{64,4},{62,8},{60,8},
            {59,4},{60,8},{62,4},{64,4},
            {60,4},{57,4},{57,4},

            {62,4},{65,8},{69,4},{67,8},{65,8},
            {64,4},{60,8},{64,4},{62,8},{60,8},
            {59,4},{59,8},{60,8},{62,4},{64,4},
            {60,4},{57,4},{57,4},

            {64,4},{59,8},{60,8},{62,4},{60,8},{59,8},
            {57,4},{57,8},{60,8},{64,4},{62,8},{60,8},
            {59,4},{60,8},{62,4},{64,4},
            {60,4},{57,4},{57,4},

            {62,4},{65,8},{69,4},{67,8},{65,8},
            {64,4},{60,8},{64,4},{62,8},{60,8},
            {59,4},{59,8},{60,8},{62,4},{64,4},
            {60,4},{57,4},{57,4},

            // Sección B
            {64,2},{60,2},
            {57,2},{62,2},
            {59,2},{55,2},
            {59,4},{60,8},{62,4},{60,8},

            {64,2},{60,2},
            {57,2},{64,4},{69,4},
            {71,2},{67,2},
            {64,4},{0,4},

            {64,2},{60,2},
            {57,2},{62,2},
            {59,2},{55,2},
            {59,4},{60,8},{62,4},{60,8},

            {64,2},{60,2},
            {57,2},{64,4},{69,4},
            {71,2},{67,2},
            {64,4},{0,4},
    };

    public Musica() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.setSequence(crearSecuencia());
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(160);
            sequencer.start();
        } catch (Exception e) {
            System.out.println("MIDI no disponible: " + e.getMessage());
        }
    }

    private Sequence crearSecuencia() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 8);
        Track track = seq.createTrack();

        // Instrumento: piano eléctrico (1)
        ShortMessage instrumento = new ShortMessage();
        instrumento.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
        track.add(new MidiEvent(instrumento, 0));

        // Volumen
        ShortMessage volumen = new ShortMessage();
        volumen.setMessage(ShortMessage.CONTROL_CHANGE, 0, 7, 100);
        track.add(new MidiEvent(volumen, 0));

        long tick = 0;
        for (int[] nota : MELODIA) {
            int pitch = nota[0];
            int durTipo = nota[1]; // 4=negra(8ticks), 8=corchea(4ticks), 2=blanca(16ticks)
            long durTicks = (durTipo == 4) ? 8 : (durTipo == 8) ? 4 : 16;

            if (pitch > 0) {
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, 0, pitch, 90);
                track.add(new MidiEvent(noteOn, tick));

                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0, pitch, 0);
                track.add(new MidiEvent(noteOff, tick + durTicks - 1));
            }
            tick += durTicks;
        }

        // Marca de fin
        MetaMessage fin = new MetaMessage();
        fin.setMessage(0x2F, new byte[]{}, 0);
        track.add(new MidiEvent(fin, tick));

        return seq;
    }

    public void toggleMute() {
        if (sequencer == null || !sequencer.isOpen()) return;
        activa = !activa;
        sequencer.setTrackMute(0, !activa);
    }

    public boolean isActiva() { return activa; }

    public void playEfecto(int tipo) {
        // tipo: 0=linea, 1=tetris, 2=gameover
        new Thread(() -> {
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                synth.open();
                MidiChannel ch = synth.getChannels()[1];

                switch (tipo) {
                    case 0: // Línea borrada
                        ch.noteOn(72, 100); Thread.sleep(80);
                        ch.noteOff(72);
                        ch.noteOn(76, 100); Thread.sleep(80);
                        ch.noteOff(76);
                        break;
                    case 1: // Tetris (4 líneas)
                        int[] acorde = {60, 64, 67, 72};
                        for (int n : acorde) ch.noteOn(n, 110);
                        Thread.sleep(300);
                        for (int n : acorde) ch.noteOff(n);
                        break;
                    case 2: // Game over
                        int[] caida = {64, 60, 57, 53, 48};
                        for (int n : caida) {
                            ch.noteOn(n, 90); Thread.sleep(120);
                            ch.noteOff(n);
                        }
                        break;
                    case 3: // Hold
                        ch.noteOn(67, 80); Thread.sleep(60);
                        ch.noteOff(67);
                        break;
                }
                Thread.sleep(100);
                synth.close();
            } catch (Exception ignored) {}
        }).start();
    }

    public void detener() {
        if (sequencer != null && sequencer.isOpen()) {
            sequencer.stop();
            sequencer.close();
        }
    }
}