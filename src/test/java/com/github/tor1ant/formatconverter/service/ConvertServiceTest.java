package com.github.tor1ant.formatconverter.service;

import com.github.tor1ant.formatconverter.TestFiles;
import com.github.tor1ant.formatconverter.exception.ConversionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ConvertServiceTest {

    private static final float A4_WIDTH = PDRectangle.A4.getWidth();
    private static final float A4_HEIGHT = PDRectangle.A4.getHeight();
    private static final float EPSILON = 0.01f;

    private final ConvertService service = new ConvertService();

    @Test
    void portraitImageFitsPortraitPageKeepingAspectRatio() throws IOException {
        // 1:2 — «выше» страницы A4 (1:1.414), значит упирается в высоту
        byte[] pdf = service.convertImageToPdf(new ByteArrayInputStream(TestFiles.png(200, 400)), "photo.png");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            PDPage page = document.getPage(0);
            assertThat(page.getMediaBox().getWidth()).isCloseTo(A4_WIDTH, within(EPSILON));
            assertThat(page.getMediaBox().getHeight()).isCloseTo(A4_HEIGHT, within(EPSILON));

            ImagePlacement placement = imagePlacement(page);
            assertThat(placement.height()).isCloseTo(A4_HEIGHT, within(EPSILON));
            assertThat(placement.width()).isCloseTo(A4_HEIGHT / 2, within(EPSILON));
            // по горизонтали изображение отцентрировано, по вертикали занимает всю страницу
            assertThat(placement.x()).isCloseTo((A4_WIDTH - placement.width()) / 2, within(EPSILON));
            assertThat(placement.y()).isCloseTo(0, within(EPSILON));
        }
    }

    @Test
    void landscapeImageGetsLandscapePage() throws IOException {
        byte[] pdf = service.convertImageToPdf(new ByteArrayInputStream(TestFiles.jpeg(400, 200)), "photo.jpg");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDPage page = document.getPage(0);
            assertThat(page.getMediaBox().getWidth()).isCloseTo(A4_HEIGHT, within(EPSILON));
            assertThat(page.getMediaBox().getHeight()).isCloseTo(A4_WIDTH, within(EPSILON));

            ImagePlacement placement = imagePlacement(page);
            assertThat(placement.width()).isCloseTo(A4_HEIGHT, within(EPSILON));
            assertThat(placement.height()).isCloseTo(A4_HEIGHT / 2, within(EPSILON));
            assertThat(placement.x()).isCloseTo(0, within(EPSILON));
            assertThat(placement.y()).isCloseTo((A4_WIDTH - placement.height()) / 2, within(EPSILON));
        }
    }

    @Test
    void squareImageIsNotStretched() throws IOException {
        byte[] pdf = service.convertImageToPdf(new ByteArrayInputStream(TestFiles.png(300, 300)), "square.png");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            ImagePlacement placement = imagePlacement(document.getPage(0));
            assertThat(placement.width()).isCloseTo(placement.height(), within(EPSILON));
            assertThat(placement.width()).isCloseTo(A4_WIDTH, within(EPSILON));
        }
    }

    @Test
    void rejectsFileThatIsNotAnImage() {
        byte[] text = "definitely not an image".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.convertImageToPdf(new ByteArrayInputStream(text), "notes.txt"))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("notes.txt")
                .hasMessageContaining("не поддерживается");
    }

    @Test
    void rejectsPdfPassedAsImage() throws IOException {
        byte[] pdf = TestFiles.pdf(1, PDRectangle.A4);

        assertThatThrownBy(() -> service.convertImageToPdf(new ByteArrayInputStream(pdf), "scan.pdf"))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("scan.pdf");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> service.convertImageToPdf(new ByteArrayInputStream(new byte[0]), "empty.png"))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("empty.png")
                .hasMessageContaining("пустой");
    }

    /**
     * Положение и размер изображения на странице. {@code drawImage(image, x, y, w, h)} записывает
     * в поток содержимого матрицу {@code w 0 0 h x y cm} — её и читаем.
     */
    private static ImagePlacement imagePlacement(PDPage page) throws IOException {
        List<Object> tokens = new PDFStreamParser(page).parse();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) instanceof Operator operator && "cm".equals(operator.getName())) {
                float width = number(tokens, i - 6);
                float height = number(tokens, i - 3);
                float x = number(tokens, i - 2);
                float y = number(tokens, i - 1);
                return new ImagePlacement(x, y, width, height);
            }
        }
        throw new AssertionError("В содержимом страницы нет оператора cm — изображение не нарисовано");
    }

    private static float number(List<Object> tokens, int index) {
        return ((COSNumber) tokens.get(index)).floatValue();
    }

    private record ImagePlacement(float x, float y, float width, float height) {
    }
}
